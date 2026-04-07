import { useState, useEffect } from 'react';
import { adminAPI } from '../services/api';
import './InfoPage.css';
import './SurveillancePage.css';




const DISEASE_COLORS = {
  'Malaria':              '#ff8c00',
  'Dengue':               '#ff4757',
  'Diarrhoea':            '#6c63ff',
  'Cholera':              '#00d4aa',
  'Typhoid':              '#ffd93d',
  'Japanese Encephalitis':'#a020f0',
  'Tuberculosis':         '#888',
  'COVID-19':             '#0099ff',
};

const SurveillancePage = () => {
  const [districts, setDistricts]   = useState([]);
  const [monDiseases, setMonDiseases] = useState([]);
  const [selected, setSelected]     = useState('');
  const [survData, setSurvData]     = useState(null);
  const [loading, setLoading]       = useState(false);
  const [loadingDistricts, setLoadingDistricts] = useState(true);
  const [error, setError]           = useState('');

  useEffect(() => {
    adminAPI.getDistricts()
      .then(res => {
        setDistricts(res.data.districts || []);
        setMonDiseases(res.data.monitoredDiseases || []);
      })
      .catch(() => setError('Failed to load district list'))
      .finally(() => setLoadingDistricts(false));
  }, []);

  const fetchSurveillance = async (district) => {
    if (!district) return;
    setLoading(true);
    setError('');
    setSurvData(null);
    try {
      const res = await adminAPI.getSurveillance(district);
      setSurvData(res.data);
    } catch {
      setError('Failed to fetch surveillance data for ' + district);
    } finally {
      setLoading(false);
    }
  };

  const handleSelect = (d) => {
    setSelected(d);
    fetchSurveillance(d);
  };

  const getMax = (data) => Math.max(...Object.values(data || {}), 1);

  return (
    <div className="page info-page">
      <div className="container">
        {/* Header */}
        <div className="page-header">
          <h1>🗺️ Disease Surveillance Dashboard</h1>
          <p>Real-time IDSP-modeled disease monitoring across all 30 Odisha districts</p>
        </div>

        {/* Monitored Diseases Banner */}
        {monDiseases.length > 0 && (
          <div className="monitored-banner">
            <span className="mb-label">🔍 Monitored Diseases:</span>
            <div className="mb-chips">
              {monDiseases.map(d => (
                <span key={d} className="mb-chip" style={{ background: `${DISEASE_COLORS[d] || '#6c63ff'}20`, color: DISEASE_COLORS[d] || '#6c63ff', borderColor: `${DISEASE_COLORS[d] || '#6c63ff'}40` }}>
                  {d}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* District Selector */}
        <div className="district-selector-wrap">
          <div className="ds-label">📍 Select District to View Weekly Data:</div>
          {loadingDistricts ? (
            <div className="loading-state"><span className="spinner" style={{ width: 24, height: 24 }} /></div>
          ) : (
            <div className="district-grid">
              {districts.map(d => (
                <button
                  key={d}
                  className={`district-btn ${selected === d ? 'district-btn-active' : ''}`}
                  onClick={() => handleSelect(d)}
                >
                  {d}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Surveillance Data Panel */}
        {loading && (
          <div className="loading-state" style={{ minHeight: 200 }}>
            <span className="spinner" style={{ width: 36, height: 36 }} />
            <p>Fetching surveillance data for {selected}…</p>
          </div>
        )}

        {error && (
          <div className="surv-error">⚠️ {error}</div>
        )}

        {survData && !loading && (
          <div className="surv-panel">
            {/* District Header */}
            <div className="surv-header">
              <div>
                <h2 className="surv-title">📊 {survData.district}, {survData.region}</h2>
                <p className="surv-source">Data Source: {survData.dataSource}</p>
              </div>
              <div className="surv-alerts-badge" style={{
                background: survData.activeAlerts > 0 ? 'rgba(255,71,87,0.15)' : 'rgba(0,212,170,0.15)',
                color: survData.activeAlerts > 0 ? '#ff4757' : '#00d4aa',
              }}>
                {survData.activeAlerts > 0
                  ? `🚨 ${survData.activeAlerts} Active Alert${survData.activeAlerts > 1 ? 's' : ''}`
                  : '✅ No Active Alerts'}
              </div>
            </div>

            {/* Bar Chart */}
            <div className="surv-chart-card">
              <div className="surv-chart-title">📈 Weekly Disease Case Counts</div>
              {survData.weeklyDiseaseCounts && Object.entries(survData.weeklyDiseaseCounts).length > 0 ? (
                <div className="surv-bars">
                  {Object.entries(survData.weeklyDiseaseCounts)
                    .sort((a, b) => b[1] - a[1])
                    .map(([disease, cases]) => {
                      const max = getMax(survData.weeklyDiseaseCounts);
                      const pct = Math.max(2, (cases / max) * 100);
                      const color = DISEASE_COLORS[disease] || '#6c63ff';
                      const isHigh = cases >= 25;
                      return (
                        <div key={disease} className="surv-bar-row">
                          <div className="surv-bar-label">{disease}</div>
                          <div className="surv-bar-track">
                            <div
                              className="surv-bar-fill"
                              style={{ width: `${pct}%`, background: color }}
                            />
                          </div>
                          <div className="surv-bar-count" style={{ color: isHigh ? color : '#888' }}>
                            {cases}
                            {isHigh && <span className="surv-spike-tag">⚠️ Spike</span>}
                          </div>
                        </div>
                      );
                    })}
                </div>
              ) : (
                <p style={{ color: '#666', fontSize: '0.88rem' }}>No case data available for this district.</p>
              )}
            </div>

            {/* Thresholds Info */}
            <div className="surv-thresholds">
              <div className="surv-thresh-title">📏 Alert Thresholds (Weekly Cases)</div>
              <div className="surv-thresh-grid">
                {[
                  { disease: 'Malaria', low: 20, high: 50, critical: 100 },
                  { disease: 'Dengue',  low: 15, high: 30, critical: 60  },
                  { disease: 'Cholera', low: 5,  high: 15, critical: 30  },
                  { disease: 'Typhoid', low: 10, high: 25, critical: 50  },
                ].map(thresh => (
                  <div key={thresh.disease} className="thresh-card">
                    <div className="thresh-name">{thresh.disease}</div>
                    <div className="thresh-vals">
                      <span style={{ color: '#00d4aa' }}>{thresh.low}</span>
                      <span style={{ color: '#ffd93d' }}>{thresh.high}</span>
                      <span style={{ color: '#ff4757' }}>{thresh.critical}</span>
                    </div>
                    <div className="thresh-labels">
                      <span style={{ color: '#00d4aa' }}>Low</span>
                      <span style={{ color: '#ffd93d' }}>High</span>
                      <span style={{ color: '#ff4757' }}>Critical</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Disclaimer */}
            <div className="surv-note">
              ℹ️ {survData.note}
            </div>
          </div>
        )}

        {/* Empty State */}
        {!selected && !loading && (
          <div className="empty-state">
            <div className="empty-icon">🗺️</div>
            <h3>Select a District</h3>
            <p>Click any of the 30 Odisha districts above to view its weekly disease surveillance data</p>
          </div>
        )}

        {/* Info Section */}
        <div className="surv-info-section">
          <h3>ℹ️ About the Surveillance System</h3>
          <div className="surv-info-grid">
            <div className="surv-info-card">
              <div className="sic-icon">🔍</div>
              <h4>Daily District Scan</h4>
              <p>Automated surveillance runs every day at 8:00 AM IST across all 30 Odisha districts</p>
            </div>
            <div className="surv-info-card">
              <div className="sic-icon">🚨</div>
              <h4>Auto Alerts</h4>
              <p>When case counts exceed thresholds, alerts are automatically created and broadcast via SMS/WhatsApp</p>
            </div>
            <div className="surv-info-card">
              <div className="sic-icon">📲</div>
              <h4>Instant Notification</h4>
              <p>Affected district residents receive localized alerts in English, Hindi, or Odia within minutes</p>
            </div>
            <div className="surv-info-card">
              <div className="sic-icon">🏥</div>
              <h4>Health Worker Alerts</h4>
              <p>HIGH and CRITICAL alerts trigger immediate notifications to registered health workers</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SurveillancePage;
