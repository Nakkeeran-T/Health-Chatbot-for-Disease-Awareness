import { Link } from 'react-router-dom';
import { useAuth } from '../context/auth';
import { useLanguage } from '../context/language';
import './LandingPage.css';

const LandingPage = () => {
  const { isAuthenticated } = useAuth();
  const { t } = useLanguage();

  const features = [
    { icon: '💬', title: t('feat1Title'), desc: t('feat1Desc') },
    { icon: '🦠', title: t('feat2Title'), desc: t('feat2Desc') },
    { icon: '💉', title: t('feat3Title'), desc: t('feat3Desc') },
    { icon: '🚨', title: t('feat4Title'), desc: t('feat4Desc') },
    { icon: '🤖', title: t('feat5Title'), desc: t('feat5Desc') },
    { icon: '📱', title: t('feat6Title'), desc: t('feat6Desc') },
  ];

  const stats = [
    { num: '170+', label: t('statDiseases') },
    { num: '20+',  label: t('statVaccines') },
    { num: '3',    label: t('statLanguages') },
    { num: '80%+', label: t('statAccuracy') },
  ];

  return (
    <div className="landing">

      {/* ── Hero ────────────────────────────────────────────────────── */}
      <section className="hero">
        <div className="hero-bg">
          <div className="hero-orb hero-orb-1"></div>
          <div className="hero-orb hero-orb-2"></div>
          <div className="hero-orb hero-orb-3"></div>
        </div>

        <div className="container hero-content">
          <div className="hero-badge">
            <span className="pulse-dot"></span>
            {t('govBadge')}
          </div>

          <div className="hero-icon-ring">
            <span className="hero-main-icon">🏥</span>
          </div>

          <h1 className="hero-title">
            {t('heroTitle')}<br />
            <span className="gradient-text">{t('heroTitleHighlight')}</span>
          </h1>

          <p className="hero-sub">{t('heroSub')}</p>

          <div className="hero-actions">
            {isAuthenticated ? (
              <Link to="/chat" className="btn btn-primary btn-lg hero-cta-btn">
                💬 {t('startChat')}
              </Link>
            ) : (
              <>
                <Link to="/register" className="btn btn-primary btn-lg hero-cta-btn">
                  🚀 {t('getStarted')}
                </Link>
                <Link to="/diseases" className="btn btn-secondary btn-lg">
                  🦠 {t('exploreDiseases')}
                </Link>
              </>
            )}
          </div>

          {/* Stats Bar */}
          <div className="hero-stats">
            {stats.map((s, i) => (
              <div key={i} className="stat-item">
                <div className="stat-num">{s.num}</div>
                <div className="stat-label">{s.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Features ────────────────────────────────────────────────── */}
      <section className="features-section">
        <div className="container">
          <div className="section-header">
            <h2 className="section-title">{t('whyMedicChat')}</h2>
            <p className="section-sub">{t('featuresSub')}</p>
          </div>
          <div className="features-grid">
            {features.map((f, i) => (
              <div key={i} className="feature-card fade-in" style={{ animationDelay: `${i * 0.08}s` }}>
                <div className="feature-icon-wrap">
                  <span className="feature-icon">{f.icon}</span>
                </div>
                <h3 className="feature-title">{f.title}</h3>
                <p className="feature-desc">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── How It Works ─────────────────────────────────────────────── */}
      <section className="how-section">
        <div className="container">
          <div className="section-header">
            <h2 className="section-title">{t('howItWorks')}</h2>
            <p className="section-sub">{t('howItWorksSub')}</p>
          </div>
          <div className="steps-grid">
            <div className="step-card">
              <div className="step-num">1</div>
              <div className="step-icon">📝</div>
              <h3>{t('stepDescribeTitle')}</h3>
              <p>{t('stepDescribeDesc')}</p>
            </div>
            <div className="step-connector">→</div>
            <div className="step-card">
              <div className="step-num">2</div>
              <div className="step-icon">🤖</div>
              <h3>{t('stepAITitle')}</h3>
              <p>{t('stepAIDesc')}</p>
            </div>
            <div className="step-connector">→</div>
            <div className="step-card">
              <div className="step-num">3</div>
              <div className="step-icon">💊</div>
              <h3>{t('stepGuidanceTitle')}</h3>
              <p>{t('stepGuidanceDesc')}</p>
            </div>
          </div>
        </div>
      </section>

      {/* ── CTA ──────────────────────────────────────────────────────── */}
      <section className="cta-section">
        <div className="container">
          <div className="cta-card">
            <div className="cta-emoji">🌟</div>
            <h2>{t('ctaTitle')}</h2>
            <p>{t('ctaSub')}</p>
            <div className="cta-actions">
              <Link to={isAuthenticated ? '/chat' : '/register'} className="btn btn-primary btn-lg">
                {isAuthenticated ? `💬 ${t('openChat')}` : `🚀 ${t('getStarted')}`}
              </Link>
              <Link to="/alerts" className="btn btn-secondary btn-lg">
                🚨 {t('viewAlerts')}
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* ── Footer ───────────────────────────────────────────────────── */}
      <footer className="footer">
        <div className="container">
          <div className="footer-logo">🏥 {t('appName')}</div>
          <p>{t('footerCopy')}</p>
          <p className="footer-sub">{t('footerEmergency')}</p>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;
