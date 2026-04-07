import { useState, useEffect, useCallback } from 'react';
import { adminAPI, diseaseAPI, vaccineAPI, alertAPI } from '../services/api';
import './AdminDashboard.css';

// ─── Reusable Modal ──────────────────────────────────────────────────────────
const Modal = ({ open, title, onClose, children }) => {
  if (!open) return null;
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{title}</h3>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
};

// ─── Stat Card ────────────────────────────────────────────────────────────────
const StatCard = ({ icon, label, value, sub, color }) => (
  <div className="stat-card" style={{ borderTop: `3px solid ${color}` }}>
    <div className="stat-icon" style={{ color }}>{icon}</div>
    <div className="stat-value" style={{ color }}>{value}</div>
    <div className="stat-label">{label}</div>
    {sub && <div className="stat-sub">{sub}</div>}
  </div>
);

// ─── Bar Chart ────────────────────────────────────────────────────────────────
const BarChart = ({ data, labelKey, valueKey, color = '#6c63ff', title }) => {
  if (!data || data.length === 0) return <div className="empty-chart">No data yet</div>;
  const max = Math.max(...data.map(d => d[valueKey] || 0), 1);
  return (
    <div className="chart-wrap">
      {title && <div className="chart-title">{title}</div>}
      <div className="bar-chart">
        {data.slice(0, 8).map((d, i) => (
          <div key={i} className="bar-row">
            <div className="bar-label" title={d[labelKey]}>{String(d[labelKey]).replace('_', ' ')}</div>
            <div className="bar-track">
              <div
                className="bar-fill"
                style={{ width: `${Math.max(2, (d[valueKey] / max) * 100)}%`, background: color }}
              />
            </div>
            <div className="bar-val">{d[valueKey]}</div>
          </div>
        ))}
      </div>
    </div>
  );
};

// ─── Donut Segment ───────────────────────────────────────────────────────────
const LangPie = ({ data }) => {
  if (!data) return null;
  const total = Object.values(data).reduce((a, b) => a + b, 0) || 1;
  const colors = { en: '#00d4aa', hi: '#ff8c00', or: '#6c63ff' };
  const labels = { en: 'English', hi: 'Hindi', or: 'Odia' };
  return (
    <div className="lang-pie">
      {Object.entries(data).map(([lang, count]) => (
        <div key={lang} className="lang-bar-row">
          <span className="lang-dot" style={{ background: colors[lang] || '#888' }} />
          <span className="lang-name">{labels[lang] || lang}</span>
          <div className="lang-track">
            <div className="lang-fill" style={{
              width: `${(count / total) * 100}%`,
              background: colors[lang] || '#888'
            }} />
          </div>
          <span className="lang-pct">{Math.round((count / total) * 100)}%</span>
        </div>
      ))}
    </div>
  );
};

// ─── Main Component ───────────────────────────────────────────────────────────
const AdminDashboard = () => {
  const [activeTab, setActiveTab] = useState('analytics');
  const [users, setUsers] = useState([]);
  const [diseases, setDiseases] = useState([]);
  const [vaccines, setVaccines] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionMsg, setActionMsg] = useState('');

  // Modals
  const [alertModal, setAlertModal] = useState({ open: false, data: null });
  const [vaccineModal, setVaccineModal] = useState({ open: false, data: null });
  const [broadcastModal, setBroadcastModal] = useState(false);
  const [whatsappModal, setWhatsappModal] = useState(false);
  const [whatsappPhone, setWhatsappPhone] = useState('');
  const [broadcastForm, setBroadcastForm] = useState({
    title: '', messageEn: '', messageHi: '', messageOr: '',
    targetAudience: 'ALL', channel: 'BOTH', district: ''
  });

  const notify = (msg) => { setActionMsg(msg); setTimeout(() => setActionMsg(''), 4000); };

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      if (activeTab === 'analytics') {
        const res = await adminAPI.getAnalytics();
        setAnalytics(res.data);
      } else if (activeTab === 'users') {
        const res = await adminAPI.getUsers();
        setUsers(res.data);
      } else if (activeTab === 'diseases') {
        const res = await diseaseAPI.getAll();
        setDiseases(res.data);
      } else if (activeTab === 'vaccines') {
        const res = await vaccineAPI.getAll();
        setVaccines(res.data);
      } else if (activeTab === 'alerts') {
        const res = await alertAPI.getAll();
        setAlerts(res.data);
      }
    } catch (e) {
      console.error(`Error fetching ${activeTab}:`, e);
      notify('⚠️ Failed to load data.');
    } finally {
      setLoading(false);
    }
  }, [activeTab]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleToggleStatus = async (id) => {
    try { await adminAPI.toggleUserStatus(id); fetchData(); }
    catch { notify('Error updating user status'); }
  };

  const handleRunScan = async () => {
    try {
      setLoading(true);
      const res = await adminAPI.runScan();
      notify(`✅ Scan complete — ${res.data.newAlertsCreated} new alerts created.`);
      fetchData();
    } catch { notify('❌ Failed to run scan.'); setLoading(false); }
  };

  const handleWeeklyBulletin = async () => {
    try { await adminAPI.runWeeklyBulletin(); notify('✅ Weekly bulletin dispatched!'); }
    catch { notify('❌ Bulletin dispatch failed.'); }
  };

  const handleTestVaccineReminder = async () => {
    try { await adminAPI.testVaccineReminder(); notify('✅ Vaccination reminder sent!'); }
    catch { notify('❌ Reminder failed. Is Twilio configured?'); }
  };

  const handleTestHealthTip = async () => {
    try { await adminAPI.testHealthTip(); notify('✅ Monthly health tip broadcast!'); }
    catch { notify('❌ Broadcast failed.'); }
  };

  const handleBroadcast = async (e) => {
    e.preventDefault();
    try {
      const res = await adminAPI.broadcast(broadcastForm);
      notify(`✅ Broadcast sent to ${res.data.sent}/${res.data.totalTargeted} users.`);
      setBroadcastModal(false);
    } catch { notify('❌ Broadcast failed.'); }
  };

  const handleWhatsappTest = async (e) => {
    e.preventDefault();
    try {
      const res = await adminAPI.testNotify(whatsappPhone);
      notify(`✅ SMS: ${res.data.smsSent}, WhatsApp: ${res.data.whatsAppSent}`);
      setWhatsappModal(false);
    } catch { notify('❌ Notification test failed.'); }
  };

  const handleSaveAlert = async (e) => {
    e.preventDefault();
    const fd = alertModal.data;
    try {
      if (fd.id) { await alertAPI.update(fd.id, fd); notify('✅ Alert updated!'); }
      else { await alertAPI.create(fd); notify('✅ Alert created!'); }
      setAlertModal({ open: false, data: null });
      fetchData();
    } catch { notify('❌ Failed to save alert.'); }
  };

  const handleSaveVaccine = async (e) => {
    e.preventDefault();
    const fd = vaccineModal.data;
    try {
      if (fd.id) { await vaccineAPI.update(fd.id, fd); notify('✅ Vaccine updated!'); }
      else { await vaccineAPI.create(fd); notify('✅ Vaccine added!'); }
      setVaccineModal({ open: false, data: null });
      fetchData();
    } catch { notify('❌ Failed to save vaccine.'); }
  };

  // ─── Renderers ────────────────────────────────────────────────────────────

  const renderAnalytics = () => {
    const a = analytics;
    if (!a) return <div className="loading-state"><span className="spinner" /><p>Loading analytics…</p></div>;

    const growthColor = a.awarenessGrowthPercent >= 0 ? '#00d4aa' : '#ff4757';
    const growthSign = a.awarenessGrowthPercent >= 0 ? '+' : '';

    return (
      <div className="analytics-layout">
        {/* KPI Row */}
        <div className="kpi-grid">
          <StatCard icon="👥" label="Total Users" value={a.totalUsers} sub={`${a.activeUsers} active`} color="#6c63ff" />
          <StatCard icon="💬" label="Total Messages" value={a.totalMessages} sub={`${a.messagesToday} today`} color="#00d4aa" />
          <StatCard icon="📅" label="Sessions" value={a.totalSessions} color="#ffd93d" />
          <StatCard icon="🦠" label="Diseases in DB" value={a.totalDiseases} color="#ff8c00" />
          <StatCard icon="💉" label="Vaccines" value={a.totalVaccines} color="#6c63ff" />
          <StatCard icon="🚨" label="Active Alerts" value={a.activeAlerts} sub={`${a.totalAlerts} total`} color="#ff4757" />
        </div>

        {/* Awareness KPI Banner */}
        <div className="kpi-banner" style={{ borderColor: growthColor }}>
          <div className="kpi-banner-icon">📈</div>
          <div>
            <div className="kpi-banner-title">Awareness Growth (30-day)</div>
            <div className="kpi-banner-value" style={{ color: growthColor }}>
              {growthSign}{a.awarenessGrowthPercent.toFixed(1)}%
              <span className="kpi-banner-target"> · Target: 20% ↑</span>
            </div>
            <div className="kpi-banner-sub">
              This month: {a.messagesThisMonth} queries · This week: {a.messagesThisWeek} · Today: {a.messagesToday}
            </div>
          </div>
          <div className={`kpi-badge ${a.awarenessGrowthPercent >= 20 ? 'kpi-pass' : 'kpi-check'}`}>
            {a.awarenessGrowthPercent >= 20 ? '✅ TARGET MET' : '🎯 IN PROGRESS'}
          </div>
        </div>

        {/* Charts Row */}
        <div className="charts-row">
          <div className="chart-card">
            <BarChart
              data={a.topIntents}
              labelKey="intent"
              valueKey="count"
              color="#6c63ff"
              title="🧠 Top Query Intents"
            />
          </div>
          <div className="chart-card">
            <BarChart
              data={a.topDistricts}
              labelKey="district"
              valueKey="users"
              color="#00d4aa"
              title="📍 Top User Districts"
            />
          </div>
        </div>

        {/* Language & Alert Severity */}
        <div className="charts-row">
          <div className="chart-card">
            <div className="chart-title">🌐 Language Breakdown</div>
            <LangPie data={a.languageBreakdown} />
          </div>
          <div className="chart-card">
            <div className="chart-title">🚨 Alerts by Severity</div>
            <div className="severity-grid">
              {a.alertsBySeverity && Object.entries(a.alertsBySeverity).map(([k, v]) => {
                const colors = { LOW: '#00d4aa', MEDIUM: '#ffd93d', HIGH: '#ff8c00', CRITICAL: '#ff4757' };
                return (
                  <div key={k} className="severity-box" style={{ borderColor: colors[k] }}>
                    <div className="severity-count" style={{ color: colors[k] }}>{v}</div>
                    <div className="severity-label">{k}</div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* System Status */}
        <div className="system-status">
          <div className="status-title">⚙️ System Status</div>
          <div className="status-row">
            <span className={`status-dot ${a.mlApiOnline ? 'dot-green' : 'dot-red'}`} />
            ML API (Flask)
          </div>
          <div className="status-row">
            <span className={`status-dot ${a.twilioReady ? 'dot-green' : 'dot-yellow'}`} />
            Twilio SMS/WhatsApp {a.twilioReady ? '(Active)' : '(Dry-run mode)'}
          </div>
          <div className="status-row">
            <span className="status-dot dot-green" />
            Spring Boot Backend (Running)
          </div>
          <div className="status-row">
            <span className="status-dot dot-green" />
            PostgreSQL Database (Connected)
          </div>
          <div className="status-last">Last scan: {a.lastScanTime}</div>
        </div>
      </div>
    );
  };

  const renderUsers = () => (
    <table className="admin-table">
      <thead><tr><th>Full Name</th><th>Username</th><th>Email</th><th>Phone</th><th>Region</th><th>Role</th><th>Status</th><th>Actions</th></tr></thead>
      <tbody>
        {users.map(user => (
          <tr key={user.id}>
            <td>{user.fullName}</td>
            <td>{user.username}</td>
            <td>{user.email}</td>
            <td>{user.phone || '—'}</td>
            <td>{user.region || '—'}</td>
            <td><span className="role-badge">{user.role}</span></td>
            <td><span className={`status-badge ${user.active ? 'status-active' : 'status-inactive'}`}>{user.active ? 'Active' : 'Inactive'}</span></td>
            <td>
              <button className="btn btn-sm btn-secondary" onClick={() => handleToggleStatus(user.id)}>
                {user.active ? 'Deactivate' : 'Activate'}
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );

  const renderDiseases = () => (
    <div>
      <p style={{ color: '#888', marginBottom: '1rem', fontSize: '0.85rem' }}>
        📚 {diseases.length} diseases in database. Use the AI chatbot or API to query them.
      </p>
      <table className="admin-table">
        <thead><tr><th>Name</th><th>Category</th><th>ICD Code</th><th>Contagious</th></tr></thead>
        <tbody>
          {diseases.map(d => (
            <tr key={d.id}>
              <td><strong>{d.name}</strong></td>
              <td>{d.category}</td>
              <td><code>{d.icdCode || '—'}</code></td>
              <td>{d.contagious ? '✅' : '❌'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  const renderVaccines = () => (
    <div>
      <div style={{ marginBottom: '1rem', display: 'flex', justifyContent: 'flex-end' }}>
        <button className="btn btn-primary btn-sm" onClick={() => setVaccineModal({ open: true, data: { vaccineName: '', disease: '', targetAge: '', numberOfDoses: 1, administrationRoute: '', doseSchedule: '', mandatoryUnderNHM: true, availability: 'Government PHC', description: '', vaccineNameHi: '', vaccineNameOr: '', descriptionHi: '', descriptionOr: '' } })}>
          ➕ Add Vaccine
        </button>
      </div>
      <table className="admin-table">
        <thead><tr><th>Vaccine Name</th><th>Disease</th><th>Target Age</th><th>Doses</th><th>Route</th><th>NHM</th><th>Actions</th></tr></thead>
        <tbody>
          {vaccines.map(v => (
            <tr key={v.id}>
              <td>{v.vaccineName}</td>
              <td>{v.disease}</td>
              <td>{v.targetAge}</td>
              <td>{v.numberOfDoses}</td>
              <td>{v.administrationRoute}</td>
              <td>{v.mandatoryUnderNHM ? <span className="badge badge-success">✓ FREE</span> : '—'}</td>
              <td className="actions">
                <button className="btn-icon" onClick={() => setVaccineModal({ open: true, data: { ...v } })}>✏️</button>
                <button className="btn-icon" onClick={async () => {
                  if (window.confirm('Delete this vaccine?')) { await vaccineAPI.delete(v.id); fetchData(); }
                }}>🗑️</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  const renderAlerts = () => (
    <div>
      <div style={{ marginBottom: '1rem', display: 'flex', flexWrap: 'wrap', gap: '8px', justifyContent: 'flex-end' }}>
        <button className="btn btn-sm" style={{ background: '#6c63ff', color: '#fff' }} onClick={handleRunScan}>🔍 Run Surveillance Scan</button>
        <button className="btn btn-sm btn-secondary" onClick={handleWeeklyBulletin}>📋 Send Weekly Bulletin</button>
        <button className="btn btn-sm" style={{ background: '#25d366', color: '#fff' }} onClick={() => setWhatsappModal(true)}>📲 Test WhatsApp/SMS</button>
        <button className="btn btn-primary btn-sm" onClick={() => setAlertModal({ open: true, data: { title: '', disease: '', description: '', region: 'Odisha', district: '', severity: 'MEDIUM', precautions: '', active: true, reportedCases: 0, contactNumber: '104' } })}>
          ➕ Create Alert
        </button>
      </div>
      <table className="admin-table">
        <thead><tr><th>Title</th><th>Disease</th><th>District</th><th>Severity</th><th>Cases</th><th>Status</th><th>Actions</th></tr></thead>
        <tbody>
          {alerts.map(a => (
            <tr key={a.id}>
              <td>{a.title}</td>
              <td>{a.disease}</td>
              <td>{a.district || '—'}</td>
              <td>
                <span className="badge" style={{
                  background: { LOW: '#00d4aa20', MEDIUM: '#ffd93d20', HIGH: '#ff8c0020', CRITICAL: '#ff475720' }[a.severity],
                  color: { LOW: '#00d4aa', MEDIUM: '#ffd93d', HIGH: '#ff8c00', CRITICAL: '#ff4757' }[a.severity]
                }}>{a.severity}</span>
              </td>
              <td>{a.reportedCases || 0}</td>
              <td>{a.active ? '🔴 Active' : '⚪ Resolved'}</td>
              <td className="actions">
                <button className="btn-icon" onClick={() => setAlertModal({ open: true, data: { ...a, severity: a.severity?.toString() } })}>✏️</button>
                <button className="btn-icon" onClick={async () => {
                  if (window.confirm('Delete this alert?')) { await alertAPI.delete(a.id); fetchData(); }
                }}>🗑️</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  const renderBroadcast = () => (
    <div className="broadcast-panel">
      <div className="broadcast-header">
        <h3>📣 Health Communication Hub</h3>
        <p>Send health tips, vaccination reminders, and outbreak alerts directly to registered users via SMS/WhatsApp</p>
      </div>
      <div className="broadcast-actions">
        <div className="broadcast-card" onClick={() => setBroadcastModal(true)}>
          <div className="bc-icon">📢</div>
          <div className="bc-title">Custom Broadcast</div>
          <div className="bc-desc">Send any health message to targeted users in their preferred language</div>
        </div>
        <div className="broadcast-card" onClick={handleTestVaccineReminder}>
          <div className="bc-icon">💉</div>
          <div className="bc-title">Vaccination Reminder</div>
          <div className="bc-desc">Send the weekly NHM vaccination schedule reminder to all users</div>
        </div>
        <div className="broadcast-card" onClick={handleTestHealthTip}>
          <div className="bc-icon">🌿</div>
          <div className="bc-title">Monthly Health Tip</div>
          <div className="bc-desc">Broadcast this month's preventive healthcare tip to all users</div>
        </div>
        <div className="broadcast-card" onClick={() => setWhatsappModal(true)}>
          <div className="bc-icon">📲</div>
          <div className="bc-title">Test WhatsApp/SMS</div>
          <div className="bc-desc">Test Twilio notification delivery to a specific phone number</div>
        </div>
      </div>

      <div className="webhook-info">
        <div className="wi-title">🔗 WhatsApp Inbound Webhook</div>
        <div className="wi-body">
          <div className="wi-row"><span>Endpoint:</span><code>POST /api/webhook/whatsapp</code></div>
          <div className="wi-row"><span>Status:</span><span className="dot-green-inline">● Active</span></div>
          <div className="wi-row"><span>Setup:</span><span>Configure in Twilio Console → WhatsApp Sandbox settings</span></div>
          <div className="wi-row"><span>Local test:</span><code>ngrok http 8080</code></div>
          <div className="wi-note">💡 Users can now WhatsApp the ArogyaBot number directly to ask health questions in English, Hindi, or Odia!</div>
        </div>
      </div>

      <div className="scheduler-info">
        <div className="wi-title">⏰ Automated Scheduler Status</div>
        <div className="scheduler-grid">
          <div className="sch-item"><span className="sch-icon">🔍</span><div><strong>Daily Surveillance Scan</strong><p>Every day at 8:00 AM IST — scans all 30 Odisha districts for disease spikes</p></div></div>
          <div className="sch-item"><span className="sch-icon">📋</span><div><strong>Weekly Health Bulletin</strong><p>Every Monday at 9:00 AM IST — sends disease summary to all users</p></div></div>
          <div className="sch-item"><span className="sch-icon">💉</span><div><strong>Vaccination Reminder</strong><p>Every Sunday at 10:00 AM IST — NHM vaccination schedule alert</p></div></div>
          <div className="sch-item"><span className="sch-icon">🌿</span><div><strong>Monthly Health Tips</strong><p>1st of every month at 10:00 AM IST — preventive healthcare tip</p></div></div>
          <div className="sch-item"><span className="sch-icon">🦟</span><div><strong>High-Risk District Scan</strong><p>Every 6 hours during monsoon (Jun–Oct) — tribal malaria districts</p></div></div>
        </div>
      </div>
    </div>
  );

  const tabs = [
    { key: 'analytics', label: '📊 Analytics' },
    { key: 'users', label: '👥 Users' },
    { key: 'diseases', label: '🦠 Diseases' },
    { key: 'vaccines', label: '💉 Vaccines' },
    { key: 'alerts', label: '🚨 Alerts' },
    { key: 'broadcast', label: '📣 Broadcast' },
  ];

  return (
    <div className="admin-dashboard page">
      <div className="container">
        <div className="admin-header">
          <div>
            <h1>⚙️ Admin Control Panel</h1>
            <p>ArogyaBot — Government of Odisha, Electronics & IT Department</p>
          </div>
          {actionMsg && <div className="action-toast">{actionMsg}</div>}
        </div>

        <div className="admin-tabs">
          {tabs.map(t => (
            <button key={t.key} className={`tab-btn ${activeTab === t.key ? 'active' : ''}`} onClick={() => setActiveTab(t.key)}>
              {t.label}
            </button>
          ))}
        </div>

        <div className="admin-content">
          {loading ? (
            <div className="loading-state"><span className="spinner" /><p>Fetching data…</p></div>
          ) : (
            <>
              {activeTab === 'analytics' && renderAnalytics()}
              {activeTab === 'users' && renderUsers()}
              {activeTab === 'diseases' && renderDiseases()}
              {activeTab === 'vaccines' && renderVaccines()}
              {activeTab === 'alerts' && renderAlerts()}
              {activeTab === 'broadcast' && renderBroadcast()}
            </>
          )}
        </div>
      </div>

      {/* ── Alert Modal ── */}
      <Modal open={alertModal.open} title={alertModal.data?.id ? '✏️ Edit Alert' : '➕ Create Alert'} onClose={() => setAlertModal({ open: false, data: null })}>
        {alertModal.data && (
          <form className="modal-form" onSubmit={handleSaveAlert}>
            <label>Title *<input required value={alertModal.data.title} onChange={e => setAlertModal(m => ({ ...m, data: { ...m.data, title: e.target.value } }))} placeholder="e.g. Malaria Outbreak in Koraput" /></label>
            <label>Disease *<input required value={alertModal.data.disease} onChange={e => setAlertModal(m => ({ ...m, data: { ...m.data, disease: e.target.value } }))} placeholder="e.g. Malaria" /></label>
            <label>Description<textarea value={alertModal.data.description} onChange={e => setAlertModal(m => ({ ...m, data: { ...m.data, description: e.target.value } }))} rows={3} placeholder="Describe the alert…" /></label>
            <div className="form-row">
              <label>Region<input value={alertModal.data.region} onChange={e => setAlertModal(m => ({ ...m, data: { ...m.data, region: e.target.value } }))} placeholder="e.g. Odisha" /></label>
              <label>District<input value={alertModal.data.district} onChange={e => setAlertModal(m => ({ ...m, data: { ...m.data, district: e.target.value } }))} placeholder="e.g. Koraput" /></label>
            </div>
            <div className="form-row">
              <label>Severity
                <select value={alertModal.data.severity} onChange={e => setAlertModal(m => ({ ...m, data: { ...m.data, severity: e.target.value } }))}>
                  {['LOW','MEDIUM','HIGH','CRITICAL'].map(s => <option key={s}>{s}</option>)}
                </select>
              </label>
              <label>Reported Cases<input type="number" min={0} value={alertModal.data.reportedCases} onChange={e => setAlertModal(m => ({ ...m, data: { ...m.data, reportedCases: parseInt(e.target.value) } }))} /></label>
            </div>
            <label>Precautions<input value={alertModal.data.precautions} onChange={e => setAlertModal(m => ({ ...m, data: { ...m.data, precautions: e.target.value } }))} placeholder="e.g. Use mosquito nets, boil water" /></label>
            <label>Contact Number<input value={alertModal.data.contactNumber} onChange={e => setAlertModal(m => ({ ...m, data: { ...m.data, contactNumber: e.target.value } }))} placeholder="104" /></label>
            <label className="checkbox-label"><input type="checkbox" checked={alertModal.data.active} onChange={e => setAlertModal(m => ({ ...m, data: { ...m.data, active: e.target.checked } }))} /> Active</label>
            <div className="modal-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setAlertModal({ open: false, data: null })}>Cancel</button>
              <button type="submit" className="btn btn-primary">💾 Save Alert</button>
            </div>
          </form>
        )}
      </Modal>

      {/* ── Vaccine Modal ── */}
      <Modal open={vaccineModal.open} title={vaccineModal.data?.id ? '✏️ Edit Vaccine' : '➕ Add Vaccine'} onClose={() => setVaccineModal({ open: false, data: null })}>
        {vaccineModal.data && (
          <form className="modal-form" onSubmit={handleSaveVaccine}>
            <label>Vaccine Name *<input required value={vaccineModal.data.vaccineName} onChange={e => setVaccineModal(m => ({ ...m, data: { ...m.data, vaccineName: e.target.value } }))} /></label>
            <label>Disease Protected<input value={vaccineModal.data.disease} onChange={e => setVaccineModal(m => ({ ...m, data: { ...m.data, disease: e.target.value } }))} /></label>
            <div className="form-row">
              <label>Target Age<input value={vaccineModal.data.targetAge} onChange={e => setVaccineModal(m => ({ ...m, data: { ...m.data, targetAge: e.target.value } }))} placeholder="e.g. At Birth" /></label>
              <label>Doses<input type="number" min={1} value={vaccineModal.data.numberOfDoses} onChange={e => setVaccineModal(m => ({ ...m, data: { ...m.data, numberOfDoses: parseInt(e.target.value) } }))} /></label>
            </div>
            <label>Route<input value={vaccineModal.data.administrationRoute} onChange={e => setVaccineModal(m => ({ ...m, data: { ...m.data, administrationRoute: e.target.value } }))} placeholder="e.g. Intramuscular" /></label>
            <label>Dose Schedule<input value={vaccineModal.data.doseSchedule} onChange={e => setVaccineModal(m => ({ ...m, data: { ...m.data, doseSchedule: e.target.value } }))} /></label>
            <label>Description<textarea value={vaccineModal.data.description} onChange={e => setVaccineModal(m => ({ ...m, data: { ...m.data, description: e.target.value } }))} rows={2} /></label>
            <label>Hindi Name<input value={vaccineModal.data.vaccineNameHi} onChange={e => setVaccineModal(m => ({ ...m, data: { ...m.data, vaccineNameHi: e.target.value } }))} /></label>
            <label>Odia Name<input value={vaccineModal.data.vaccineNameOr} onChange={e => setVaccineModal(m => ({ ...m, data: { ...m.data, vaccineNameOr: e.target.value } }))} /></label>
            <label className="checkbox-label"><input type="checkbox" checked={vaccineModal.data.mandatoryUnderNHM} onChange={e => setVaccineModal(m => ({ ...m, data: { ...m.data, mandatoryUnderNHM: e.target.checked } }))} /> Mandatory under NHM (FREE)</label>
            <div className="modal-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setVaccineModal({ open: false, data: null })}>Cancel</button>
              <button type="submit" className="btn btn-primary">💾 Save Vaccine</button>
            </div>
          </form>
        )}
      </Modal>

      {/* ── Broadcast Modal ── */}
      <Modal open={broadcastModal} title="📢 Custom Broadcast" onClose={() => setBroadcastModal(false)}>
        <form className="modal-form" onSubmit={handleBroadcast}>
          <label>Title<input value={broadcastForm.title} onChange={e => setBroadcastForm(f => ({ ...f, title: e.target.value }))} placeholder="e.g. Important Health Alert" /></label>
          <label>Message (English) *<textarea required value={broadcastForm.messageEn} onChange={e => setBroadcastForm(f => ({ ...f, messageEn: e.target.value }))} rows={4} placeholder="Your health message in English…" /></label>
          <label>Message (Hindi)<textarea value={broadcastForm.messageHi} onChange={e => setBroadcastForm(f => ({ ...f, messageHi: e.target.value }))} rows={3} placeholder="हिंदी में संदेश (optional)" /></label>
          <label>Message (Odia)<textarea value={broadcastForm.messageOr} onChange={e => setBroadcastForm(f => ({ ...f, messageOr: e.target.value }))} rows={3} placeholder="ଓଡ଼ିଆ ସଂଦେଶ (optional)" /></label>
          <div className="form-row">
            <label>Audience
              <select value={broadcastForm.targetAudience} onChange={e => setBroadcastForm(f => ({ ...f, targetAudience: e.target.value }))}>
                <option value="ALL">All Users</option>
                <option value="USERS">Registered Users</option>
                <option value="HEALTH_WORKERS">Health Workers</option>
              </select>
            </label>
            <label>Channel
              <select value={broadcastForm.channel} onChange={e => setBroadcastForm(f => ({ ...f, channel: e.target.value }))}>
                <option value="BOTH">SMS + WhatsApp</option>
                <option value="WHATSAPP">WhatsApp Only</option>
                <option value="SMS">SMS Only</option>
              </select>
            </label>
          </div>
          <label>District (optional — leave blank for all)<input value={broadcastForm.district} onChange={e => setBroadcastForm(f => ({ ...f, district: e.target.value }))} placeholder="e.g. Koraput" /></label>
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={() => setBroadcastModal(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary">📤 Send Broadcast</button>
          </div>
        </form>
      </Modal>

      {/* ── WhatsApp Test Modal ── */}
      <Modal open={whatsappModal} title="📲 Test WhatsApp / SMS" onClose={() => setWhatsappModal(false)}>
        <form className="modal-form" onSubmit={handleWhatsappTest}>
          <p style={{ color: '#aaa', margin: '0 0 1rem' }}>
            Sends a test SMS and WhatsApp message via Twilio to verify connectivity.
          </p>
          <label>Phone Number (E.164 format) *
            <input required value={whatsappPhone} onChange={e => setWhatsappPhone(e.target.value)}
              placeholder="+919876543210" />
          </label>
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={() => setWhatsappModal(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary" style={{ background: '#25d366' }}>📲 Send Test</button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default AdminDashboard;
