import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/auth';
import { useLanguage } from '../context/language';
import './Navbar.css';

const Navbar = () => {
  const { user, logout, isAuthenticated, isAdmin } = useAuth();
  const { t, language, changeLanguage, SUPPORTED_LANGUAGES } = useLanguage();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/');
    setMenuOpen(false);
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand">
          <span className="brand-icon">🏥</span>
          <span className="brand-name">{t('appName')}</span>
          <span className="brand-badge">AI</span>
        </Link>

        <div className={`navbar-links ${menuOpen ? 'open' : ''}`}>
          <Link to="/" className={`nav-link ${isActive('/') ? 'active' : ''}`} onClick={() => setMenuOpen(false)}>
            {t('home')}
          </Link>
          {isAuthenticated && (
            <Link to="/chat" className={`nav-link ${isActive('/chat') ? 'active' : ''}`} onClick={() => setMenuOpen(false)}>
              💬 {t('chat')}
            </Link>
          )}
          <Link to="/diseases" className={`nav-link ${isActive('/diseases') ? 'active' : ''}`} onClick={() => setMenuOpen(false)}>
            🦠 {t('diseases')}
          </Link>
          <Link to="/vaccines" className={`nav-link ${isActive('/vaccines') ? 'active' : ''}`} onClick={() => setMenuOpen(false)}>
            💉 {t('vaccines')}
          </Link>
          <Link to="/alerts" className={`nav-link ${isActive('/alerts') ? 'active' : ''}`} onClick={() => setMenuOpen(false)}>
            🚨 {t('alerts')}
          </Link>
          <Link to="/surveillance" className={`nav-link ${isActive('/surveillance') ? 'active' : ''}`} onClick={() => setMenuOpen(false)}>
            🗺️ Surveillance
          </Link>
          {isAuthenticated && isAdmin() && (
            <Link to="/admin" className={`nav-link admin-link ${isActive('/admin') ? 'active' : ''}`} onClick={() => setMenuOpen(false)}>
              ⚙️ Admin
            </Link>
          )}

          <div className="lang-selector">
            {Object.entries(SUPPORTED_LANGUAGES).map(([code, info]) => (
              <button
                key={code}
                className={`lang-btn ${language === code ? 'active' : ''}`}
                onClick={() => changeLanguage(code)}
                title={info.nativeLabel}
              >
                {info.label}
              </button>
            ))}
          </div>

          {isAuthenticated ? (
            <div className="user-menu">
              <span className="user-name">👤 {user?.fullName?.split(' ')[0]}</span>
              <button className="btn btn-secondary btn-sm" onClick={handleLogout}>
                {t('logout')}
              </button>
            </div>
          ) : (
            <div className="auth-links">
              <Link to="/login" className="btn btn-secondary btn-sm" onClick={() => setMenuOpen(false)}>
                {t('login')}
              </Link>
              <Link to="/register" className="btn btn-primary btn-sm" onClick={() => setMenuOpen(false)}>
                {t('register')}
              </Link>
            </div>
          )}
        </div>

        <button className="hamburger" onClick={() => setMenuOpen(!menuOpen)}>
          {menuOpen ? '✕' : '☰'}
        </button>
      </div>
    </nav>
  );
};

export default Navbar;
