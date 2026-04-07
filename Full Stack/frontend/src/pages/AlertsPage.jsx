import { useState, useEffect } from 'react';
import { alertAPI } from '../services/api';
import { useLanguage } from '../context/language';
import './InfoPage.css';

const severityConfig = {
  LOW:      { color: '#00d4aa', bg: 'rgba(0,212,170,0.1)',  label: '🟢 LOW',      icon: '📋' },
  MEDIUM:   { color: '#ffd93d', bg: 'rgba(255,217,61,0.1)', label: '🟡 MEDIUM',   icon: '⚠️' },
  HIGH:     { color: '#ff8c00', bg: 'rgba(255,140,0,0.1)',  label: '🟠 HIGH',     icon: '🔶' },
  CRITICAL: { color: '#ff4757', bg: 'rgba(255,71,87,0.1)',  label: '🔴 CRITICAL', icon: '🚨' },
};

const AlertsPage = () => {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const { t, language } = useLanguage();

  useEffect(() => {
    alertAPI.getActive().then(res => setAlerts(res.data)).finally(() => setLoading(false));
  }, []);

  const getTitle = (a) => {
    if (language === 'hi' && a.titleHi) return a.titleHi;
    if (language === 'or' && a.titleOr) return a.titleOr;
    return a.title;
  };

  const getDesc = (a) => {
    if (language === 'hi' && a.descriptionHi) return a.descriptionHi;
    if (language === 'or' && a.descriptionOr) return a.descriptionOr;
    return a.description;
  };

  const formatDate = (str) =>
    str ? new Date(str).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' }) : '';

  const emergencyNumbers = [
    { num: '108', label: t('ambulanceNum'),       sub: t('ambulanceSub') },
    { num: '104', label: t('healthHelplineNum'),  sub: t('healthHelplineSub') },
    { num: '1075',label: t('covidHelpline'),      sub: t('covidHelplineSub') },
    { num: '100', label: t('police'),             sub: t('policeSub') },
  ];

  return (
    <div className="page info-page">
      <div className="container">
        <div className="page-header">
          <h1>🚨 {t('alertsTitle')}</h1>
          <p>{t('alertsSub')}</p>
        </div>

        {/* Summary bar */}
        <div className="alert-summary">
          <div className="summary-item">
            <div className="summary-num" style={{ color: '#ff4757' }}>{alerts.length}</div>
            <div className="summary-label">{t('activeAlerts')}</div>
          </div>
          {Object.entries(severityConfig).map(([key, cfg]) => (
            <div key={key} className="summary-item">
              <div className="summary-num" style={{ color: cfg.color }}>
                {alerts.filter(a => a.severity === key).length}
              </div>
              <div className="summary-label">{cfg.icon} {key}</div>
            </div>
          ))}
        </div>

        {loading ? (
          <div className="loading-grid">
            {[...Array(3)].map((_, i) => <div key={i} className="skeleton-card"></div>)}
          </div>
        ) : alerts.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">✅</div>
            <h3>{t('noActiveAlerts')}</h3>
            <p>{t('allRegionsClear')}</p>
          </div>
        ) : (
          <div className="alerts-grid">
            {alerts.map((alert) => {
              const cfg = severityConfig[alert.severity] || severityConfig.LOW;
              return (
                <div key={alert.id} className="alert-card" style={{ borderLeft: `4px solid ${cfg.color}` }}>
                  <div className="alert-card-header">
                    <div className="alert-disease">
                      <span className="alert-icon">{cfg.icon}</span>
                      <span className="alert-disease-name">{alert.disease}</span>
                    </div>
                    <span className="badge" style={{ background: cfg.bg, color: cfg.color, borderColor: `${cfg.color}40` }}>
                      {alert.severity}
                    </span>
                  </div>

                  <h3 className="alert-title">{getTitle(alert)}</h3>
                  <p className="alert-desc">{getDesc(alert)}</p>

                  <div className="alert-meta">
                    <div className="alert-meta-item">
                      <span className="meta-label">📍 {t('region')}</span>
                      <span className="meta-val">{alert.district}, {alert.region}</span>
                    </div>
                    <div className="alert-meta-item">
                      <span className="meta-label">🦠 {t('cases')}</span>
                      <span className="meta-val" style={{ color: cfg.color }}>{alert.reportedCases}</span>
                    </div>
                    <div className="alert-meta-item">
                      <span className="meta-label">📅 {t('reported')}</span>
                      <span className="meta-val">{formatDate(alert.reportedAt)}</span>
                    </div>
                    {alert.contactNumber && (
                      <div className="alert-meta-item">
                        <span className="meta-label">📞 {t('helpline')}</span>
                        <span className="meta-val" style={{ color: '#00d4aa' }}>{alert.contactNumber}</span>
                      </div>
                    )}
                  </div>

                  {alert.precautions && (
                    <div className="alert-precautions">
                      <strong>🛡️ {t('precautions')}:</strong> {alert.precautions}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {/* Emergency info */}
        <div className="emergency-section">
          <h3>{t('emergencyContacts2')}</h3>
          <div className="emergency-grid">
            {emergencyNumbers.map((e) => (
              <div key={e.num} className="emergency-box">
                <div className="emg-num">{e.num}</div>
                <div className="emg-label">{e.label}</div>
                <div className="emg-sub">{e.sub}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AlertsPage;
