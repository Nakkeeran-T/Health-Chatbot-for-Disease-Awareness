import { useState, useEffect } from 'react';
import { AuthContext } from './auth';

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const savedToken = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');
    if (savedToken && savedUser) {
      setToken(savedToken);
      setUser(JSON.parse(savedUser));
    }
    setLoading(false);
  }, []);

  const login = (authData) => {
    localStorage.setItem('token', authData.token);
    localStorage.setItem('user', JSON.stringify({
      username: authData.username,
      fullName: authData.fullName,
      email: authData.email,
      role: authData.role,
      preferredLanguage: authData.preferredLanguage,
    }));
    setToken(authData.token);
    setUser({
      username: authData.username,
      fullName: authData.fullName,
      email: authData.email,
      role: authData.role,
      preferredLanguage: authData.preferredLanguage,
    });
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  };

  const isAdmin = () => user?.role === 'ADMIN';

  return (
    <AuthContext.Provider value={{ user, token, login, logout, loading, isAuthenticated: !!token, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
};
