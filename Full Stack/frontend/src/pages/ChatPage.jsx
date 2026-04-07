import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { chatAPI } from '../services/api';
import { useAuth } from '../context/auth';
import { useLanguage } from '../context/language';
import ReactMarkdown from 'react-markdown';
import './ChatPage.css';

// Translation keys for the quick-reply chips — resolved via t() at render time
const QUICK_REPLY_KEYS = [
  ['qr_malaria', 'qr_vaccine', 'qr_alerts', 'qr_prevention'],
  ['qr_dengue', 'qr_tb', 'qr_cholera', 'qr_child', 'qr_covid'],
];

// BCP-47 locale tags used by the Web Speech API
const SPEECH_LOCALES = { en: 'en-IN', hi: 'hi-IN', or: 'or-IN' };

const BotTyping = () => (
  <div className="chat-msg bot">
    <div className="msg-avatar">🤖</div>
    <div className="msg-bubble">
      <div className="typing-indicator">
        <span></span><span></span><span></span>
      </div>
    </div>
  </div>
);

const ChatPage = () => {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [suggestionKeys, setSuggestionKeys] = useState(QUICK_REPLY_KEYS[0]);
  const [langMenuOpen, setLangMenuOpen] = useState(false);
  const [attachMenuOpen, setAttachMenuOpen] = useState(false);
  const [isWebcamOpen, setIsWebcamOpen] = useState(false);
  const [listening, setListening] = useState(false);   // voice state
  const recognitionRef = useRef(null);
  const messagesEndRef = useRef(null);
  const fileInputRef = useRef(null);
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const imageUrlsRef = useRef([]);
  const { isAuthenticated, user } = useAuth();
  const { t, language, changeLanguage, SUPPORTED_LANGUAGES } = useLanguage();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isAuthenticated) { navigate('/login'); return; }
    setMessages([{
      id: Date.now(),
      type: 'bot',
      content: t('chatWelcome', { name: user?.fullName || '' }),
    }]);
  }, [isAuthenticated, language, navigate, user?.fullName, t]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  useEffect(() => {
    // Cleanup webcam stream and image object URLs on unmount
    const stream = streamRef.current;
    const imageUrls = imageUrlsRef.current;
    return () => {
      if (stream) {
        stream.getTracks().forEach(track => track.stop());
      }
      imageUrls.forEach(url => URL.revokeObjectURL(url));
    };
  }, []);

  const openWebcam = async () => {
    setAttachMenuOpen(false);
    setIsWebcamOpen(true);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
    } catch (err) {
      console.error("Webcam error:", err);
      alert("Could not access the camera. Please allow permission.");
      setIsWebcamOpen(false);
    }
  };

  const closeWebcam = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }
    setIsWebcamOpen(false);
  };

  const capturePhoto = () => {
    if (!videoRef.current) return;
    const canvas = document.createElement("canvas");
    canvas.width = videoRef.current.videoWidth;
    canvas.height = videoRef.current.videoHeight;
    const ctx = canvas.getContext("2d");
    // Mirror the image to match the video preview if desired, but default is fine
    ctx.drawImage(videoRef.current, 0, 0, canvas.width, canvas.height);
    
    canvas.toBlob((blob) => {
      if (!blob) return;
      const file = new File([blob], "webcam_capture.jpg", { type: "image/jpeg" });
      closeWebcam();
      handleImageUpload({ target: { files: [file] } });
    }, "image/jpeg", 0.9);
  };

  const sendMessage = async (text) => {
    const msg = text || input.trim();
    if (!msg) return;

    setInput('');
    setMessages(prev => [...prev, { id: Date.now(), type: 'user', content: msg }]);
    setLoading(true);

    try {
      const res = await chatAPI.sendMessage({ message: msg, language });
      // sessionId is ignored since we are hitting ML API directly
      // If the API returns custom suggestions use them, otherwise keep translated defaults
      if (res.data.suggestions?.length) setSuggestionKeys(res.data.suggestions);
      
      // The ML API returns .response and .confidence directly
      const botResponse = res.data.response || res.data.botResponse || "Sorry, no response received.";
      const intentFound = res.data.intent || 'unknown';
      const confScore = res.data.confidence || res.data.confidenceScore || 0;

      setMessages(prev => [...prev, {
        id: Date.now() + 1,
        type: 'bot',
        content: botResponse,
        intent: intentFound,
        confidence: confScore,
      }]);
    } catch (err) {
      console.error('Chat API error:', err);
      setMessages(prev => [...prev, {
        id: Date.now() + 1,
        type: 'bot',
        content: t('chatError'),
      }]);
    } finally {
      setLoading(false);
    }
  };

  const handleImageUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Create a preview URL and track it for cleanup
    const imageUrl = URL.createObjectURL(file);
    imageUrlsRef.current.push(imageUrl);

    setMessages(prev => [...prev, {
      id: Date.now(),
      type: 'user',
      content: file.name,
      imageUrl,
    }]);
    setLoading(true);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const res = await chatAPI.sendImage(formData);
      const confPercent = Math.round(res.data.confidence * 100);
      
      const botResponse = `Based on the image analysis, my estimate is **${res.data.disease}** (Confidence: ${confPercent}%).\n\n` +
           `🛡️ **Prevention:** ${res.data.prevention}\n` +
           `💊 **Treatment:** ${res.data.treatment}\n` +
           `📞 **Helpline:** ${res.data.helpline}\n\n` +
           `⚠️ *Disclaimer: This is an AI prediction and not professional medical advice. Please consult a doctor.*`;

      setMessages(prev => [...prev, {
        id: Date.now() + 1,
        type: 'bot',
        content: botResponse,
        intent: 'image_prediction',
        confidence: res.data.confidence,
      }]);
    } catch (err) {
      console.error('Chat Image API error:', err);
      setMessages(prev => [...prev, {
        id: Date.now() + 1,
        type: 'bot',
        content: 'Sorry, I could not process the image at this moment. Please ensure the ML service is running and try again.',
      }]);
    } finally {
      setLoading(false);
      setAttachMenuOpen(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); }
  };

  /* ── Voice input ── */
  const toggleVoice = () => {
    const SpeechRecognition =
      window.SpeechRecognition || window.webkitSpeechRecognition;

    if (!SpeechRecognition) {
      alert(t('voiceNotSupported'));
      return;
    }

    // If already listening, stop it
    if (listening) {
      recognitionRef.current?.stop();
      setListening(false);
      return;
    }

    const rec = new SpeechRecognition();
    rec.lang = SPEECH_LOCALES[language] || 'en-IN';
    rec.interimResults = true;   // show partial results live
    rec.continuous = false;
    rec.maxAlternatives = 1;

    rec.onstart = () => setListening(true);

    rec.onresult = (e) => {
      let transcript = '';
      for (let i = e.resultIndex; i < e.results.length; i++) {
        transcript += e.results[i][0].transcript;
      }
      setInput(transcript);   // update textarea live
    };

    rec.onerror = (e) => {
      console.error('Speech error:', e.error);
      if (e.error !== 'no-speech') alert(t('voiceError'));
      setListening(false);
    };

    rec.onend = () => setListening(false);

    recognitionRef.current = rec;
    rec.start();
  };

  // Stop recognition whenever language changes
  useEffect(() => {
    if (listening) {
      recognitionRef.current?.stop();
      setListening(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [language]);

  return (
    <div className="chat-page">
      {/* Sidebar */}
      <div className="chat-sidebar">
        <div className="sidebar-header">
          <h3>🏥 {t('chatTitle')}</h3>
          <p>{t('chatSubtitle')}</p>
        </div>
        <div className="sidebar-section">
          <p className="sidebar-label">{t('quickTopics')}</p>
          {QUICK_REPLY_KEYS[1].map((key) => {
            const label = t(key);
            return (
              <button key={key} className="sidebar-btn" onClick={() => sendMessage(label)}>
                {label}
              </button>
            );
          })}
        </div>
        <div className="sidebar-section">
          <p className="sidebar-label">{t('emergencyContacts')}</p>
          <div className="emergency-card">
            <div className="emergency-num">📞 108</div>
            <div className="emergency-sub">{t('ambulance')}</div>
          </div>
          <div className="emergency-card">
            <div className="emergency-num">📞 104</div>
            <div className="emergency-sub">{t('healthHelpline')}</div>
          </div>
        </div>
      </div>

      {/* Chat main */}
      <div className="chat-main">
        <div className="chat-header">
          <div className="chat-header-info">
            <div className="bot-avatar">🤖</div>
            <div>
              <div className="bot-name">{t('chatTitle')}</div>
              <div className="bot-status"><span className="status-dot"></span> {t('online')}</div>
            </div>
          </div>

          {/* ── In-chat language switcher ── */}
          <div className="chat-lang-switcher" style={{ position: 'relative' }}>
            <button
              id="chat-lang-btn"
              className="lang-switch-btn"
              onClick={() => setLangMenuOpen(o => !o)}
              title={t('switchLang')}
            >
              🌐 {SUPPORTED_LANGUAGES[language]?.nativeLabel}
              <span className="lang-caret">{langMenuOpen ? '▲' : '▼'}</span>
            </button>
            {langMenuOpen && (
              <div className="lang-dropdown">
                {Object.entries(SUPPORTED_LANGUAGES).map(([code, info]) => (
                  <button
                    key={code}
                    className={`lang-option${language === code ? ' active' : ''}`}
                    onClick={() => {
                      changeLanguage(code);
                      setLangMenuOpen(false);
                    }}
                  >
                    {info.nativeLabel}
                    {language === code && <span className="lang-check">✓</span>}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="chat-messages">
          {messages.map((msg) => (
            <div key={msg.id} className={`chat-msg ${msg.type}`}>
              {msg.type === 'bot' && <div className="msg-avatar">🤖</div>}
              <div className="msg-bubble">
                {msg.imageUrl ? (
                  <div className="msg-image-wrap">
                    <img
                      src={msg.imageUrl}
                      alt={msg.content}
                      className="msg-image-preview"
                    />
                    <div className="msg-image-caption">📎 {msg.content}</div>
                  </div>
                ) : (
                  <ReactMarkdown>{msg.content}</ReactMarkdown>
                )}
                {msg.confidence && (
                  <div className="msg-meta">
                    {t('intent')}: {msg.intent} · {Math.round(msg.confidence * 100)}% {t('confidence')}
                  </div>
                )}
              </div>
              {msg.type === 'user' && <div className="msg-avatar user-avatar">👤</div>}
            </div>
          ))}
          {loading && <BotTyping />}
          <div ref={messagesEndRef} />
        </div>

        {/* Suggestions — translated via t() */}
        <div className="suggestions">
          {suggestionKeys.map((key, i) => {
            const label = t(key) !== key ? t(key) : key; // fallback: show raw if not a t() key
            return (
              <button key={i} className="suggestion-btn" onClick={() => sendMessage(label)}>
                {label}
              </button>
            );
          })}
        </div>

        {/* Input */}
        <div className="chat-input-wrapper">
          {listening && (
            <div className="voice-listening-bar">
              <span className="voice-pulse"></span>
              <span className="voice-pulse"></span>
              <span className="voice-pulse"></span>
              <span className="voice-listening-label">{t('voiceListening')}</span>
            </div>
          )}
          <div className="chat-input-bar">
            {/* Hidden file inputs */}
            <input 
              type="file" 
              accept="image/*" 
              ref={fileInputRef} 
              style={{ display: 'none' }} 
              onChange={handleImageUpload} 
            />

            {/* Attach "+" Button and Menu */}
            <div className="attach-container" style={{ position: 'relative' }}>
              <button
                className="attach-toggle-btn"
                onClick={() => setAttachMenuOpen(!attachMenuOpen)}
                disabled={loading}
                title="Add Attachment"
                style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '0.4rem', display: 'flex', alignItems: 'center', color: 'var(--text-light, #6b7280)' }}
              >
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ transition: 'transform 0.2s', transform: attachMenuOpen ? 'rotate(45deg)' : 'rotate(0)' }}>
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
              </button>

              {attachMenuOpen && (
                <div className="attach-menu">
                  <div className="attach-menu-header">Add context</div>
                  <button className="attach-menu-item" onClick={() => { fileInputRef.current?.click(); setAttachMenuOpen(false); }}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                      <circle cx="8.5" cy="8.5" r="1.5"></circle>
                      <polyline points="21 15 16 10 5 21"></polyline>
                    </svg>
                    <span>Gallery</span>
                  </button>
                  <button className="attach-menu-item" onClick={openWebcam}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                      <circle cx="12" cy="13" r="4"></circle>
                    </svg>
                    <span>Camera</span>
                  </button>
                </div>
              )}
            </div>

            {/* Mic button */}
            <button
              id="voice-input-btn"
              className={`mic-btn${listening ? ' mic-active' : ''}`}
              onClick={toggleVoice}
              disabled={loading}
              title={t('voiceBtn')}
              aria-label={t('voiceBtn')}
            >
              {listening ? '⏹' : '🎤'}
            </button>

            <textarea
              className={`chat-input${listening ? ' input-listening' : ''}`}
              lang={SPEECH_LOCALES[language]?.split('-')[0] ?? 'en'}
              dir="auto"
              placeholder={listening ? t('voiceInput') : t('typePlaceholder')}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              rows={1}
              disabled={loading}
            />
            <button
              className="send-btn"
              onClick={() => sendMessage()}
              disabled={loading || !input.trim()}
            >
              {loading ? <span className="spinner"></span> : '➤'}
            </button>
          </div>
        </div>
      </div>

      {/* Webcam Modal */}
      {isWebcamOpen && (
        <div className="webcam-modal-overlay">
          <div className="webcam-modal-content">
            <div className="webcam-header">
              <h3>Take Photo</h3>
              <button className="close-webcam-btn" onClick={closeWebcam}>✕</button>
            </div>
            <div className="webcam-video-container">
              <video 
                ref={videoRef} 
                autoPlay 
                playsInline 
                muted
                className="webcam-video" 
              />
            </div>
            <div className="webcam-actions">
              <button className="webcam-capture-btn" onClick={capturePhoto}>
                <div className="webcam-capture-inner"></div>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ChatPage;
