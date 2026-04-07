import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authAPI } from '../services/api';
import { useAuth } from '../context/auth';
import { useLanguage } from '../context/language';
import './AuthPage.css';

const LoginPage = () => {
  const [form, setForm] = useState({ username: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const { login } = useAuth();
  const { t } = useLanguage();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const res = await authAPI.login(form);
      login(res.data);
      navigate('/chat');
    } catch (err) {
      setError(err.response?.data?.error || t('loginFailed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-logo">🏥</div>
          <h1>{t('welcomeBack')}</h1>
          <p>{t('loginSub')}</p>
        </div>

        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="input-group">
            <label>{t('username')}</label>
            <input
              className="input-field"
              type="text"
              placeholder={t('usernamePlaceholder')}
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              required
            />
          </div>
          <div className="input-group">
            <label>{t('password')}</label>
            <input
              className="input-field"
              type="password"
              placeholder={t('passwordPlaceholder')}
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
            />
          </div>

          <button type="submit" className="btn btn-primary auth-btn" disabled={loading}>
            {loading ? <><span className="spinner"></span> {t('signingIn')}</> : t('signIn')}
          </button>
        </form>

        <div className="auth-demo">
          <p>{t('demoAccounts')}</p>
          <div className="demo-btns">
            <button className="demo-btn" onClick={() => setForm({ username: 'demo', password: 'demo123' })}>
              User: demo / demo123
            </button>
            <button className="demo-btn" onClick={() => setForm({ username: 'admin', password: 'admin123' })}>
              Admin: admin / admin123
            </button>
          </div>
        </div>

        <p className="auth-switch">
          {t('noAccount')} <Link to="/register">{t('registerHere')}</Link>
        </p>
      </div>
    </div>
  );
};

export default LoginPage;
