import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { useAuth } from './context/auth';
import { LanguageProvider } from './context/LanguageContext';
import Navbar from './components/Navbar';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ChatPage from './pages/ChatPage';
import DiseasesPage from './pages/DiseasesPage';
import VaccinePage from './pages/VaccinePage';
import AlertsPage from './pages/AlertsPage';
import AdminDashboard from './pages/AdminDashboard';
import SurveillancePage from './pages/SurveillancePage';


const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();
  if (loading) return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh' }}>
      <span className="spinner" style={{ width: 40, height: 40 }}></span>
    </div>
  );
  return isAuthenticated ? children : <Navigate to="/login" replace />;
};

const AdminRoute = ({ children }) => {
  const { isAuthenticated, isAdmin, loading } = useAuth();
  if (loading) return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh' }}>
      <span className="spinner" style={{ width: 40, height: 40 }}></span>
    </div>
  );
  return isAuthenticated && isAdmin() ? children : <Navigate to="/" replace />;
};

const AppRoutes = () => (
  <>
    <Navbar />
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/diseases" element={<DiseasesPage />} />
      <Route path="/vaccines" element={<VaccinePage />} />
      <Route path="/alerts" element={<AlertsPage />} />
      <Route path="/surveillance" element={<SurveillancePage />} />
      <Route path="/chat" element={
        <ProtectedRoute>
          <ChatPage />
        </ProtectedRoute>
      } />
      <Route path="/admin" element={
        <AdminRoute>
          <AdminDashboard />
        </AdminRoute>
      } />
      <Route path="*" element={<Navigate to="/" replace />} />

    </Routes>
  </>
);

const App = () => (
  <BrowserRouter>
    <AuthProvider>
      <LanguageProvider>
        <AppRoutes />
      </LanguageProvider>
    </AuthProvider>
  </BrowserRouter>
);

export default App;
