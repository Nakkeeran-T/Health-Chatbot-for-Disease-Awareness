import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authAPI } from '../services/api';
import { useAuth } from '../context/auth';
import { useLanguage } from '../context/language';
import './AuthPage.css';

const RegisterPage = () => {
  const [form, setForm] = useState({
    username: '', password: '', email: '', fullName: '',
    phone: '', district: '', preferredLanguage: 'en'
  });
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
      const res = await authAPI.register(form);
      login(res.data);
      navigate('/chat');
    } catch (err) {
      setError(err.response?.data?.error || t('registrationFailed'));
    } finally {
      setLoading(false);
    }
  };

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  return (
    <div className="auth-page">
      <div className="auth-card auth-card-lg">
        <div className="auth-header">
          <div className="auth-logo">🏥</div>
          <h1>{t('createAccount')}</h1>
          <p>{t('registerSub')}</p>
        </div>

        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-grid-2">
            <div className="input-group">
              <label>{t('fullName')}</label>
              <input className="input-field" type="text" placeholder={t('fullNamePlaceholder')} value={form.fullName} onChange={update('fullName')} required />
            </div>
            <div className="input-group">
              <label>{t('usernameStar')}</label>
              <input className="input-field" type="text" placeholder={t('chooseUsername')} value={form.username} onChange={update('username')} required />
            </div>
          </div>
          <div className="input-group">
            <label>{t('emailStar')}</label>
            <input className="input-field" type="email" placeholder={t('emailPlaceholder')} value={form.email} onChange={update('email')} required />
          </div>
          <div className="input-group">
            <label>{t('passwordStar')}</label>
            <input className="input-field" type="password" placeholder={t('passwordMinPlaceholder')} value={form.password} onChange={update('password')} required minLength={6} />
          </div>
          <div className="form-grid-2">
            <div className="input-group">
              <label>{t('phone')}</label>
              <input className="input-field" type="tel" placeholder={t('phonePlaceholder')} value={form.phone} onChange={update('phone')} />
            </div>
            <div className="input-group">
              <label>{t('regionDistrict')}</label>
              <input className="input-field" type="text" placeholder={t('regionPlaceholder')} value={form.district} onChange={update('district')} />
            </div>
          </div>
          <div className="input-group">
            <label>{t('preferredLanguage')}</label>
            <select className="input-field" value={form.preferredLanguage} onChange={update('preferredLanguage')}>
              <option value="en">{t('langEnglish')}</option>
              <option value="hi">{t('langHindi')}</option>
              <option value="or">{t('langOdia')}</option>
            </select>
          </div>

          <button type="submit" className="btn btn-primary auth-btn" disabled={loading}>
            {loading ? <><span className="spinner"></span> {t('creatingAccount')}</> : t('createAccountBtn')}
          </button>
        </form>

        <p className="auth-switch">
          {t('alreadyHaveAccount')} <Link to="/login">{t('signInHere')}</Link>
        </p>
      </div>
    </div>
  );
};

export default RegisterPage;
