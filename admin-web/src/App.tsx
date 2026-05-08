import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import AdminLayout from './components/Layout/AdminLayout';
import Dashboard from './components/Dashboard/Dashboard';
import Knowledge from './components/Knowledge/Knowledge';
import NotificationComponent from './components/Notification/Notification';
import AvatarStudio from './components/AvatarStudio';
import Interactions from './pages/Interactions';
import StatisticsPage from './pages/Statistics';
import Login from './pages/Login';
import { useAdminStore } from './store';

const RequireAuth: React.FC<{ children: React.ReactElement }> = ({ children }) => {
  const { isAuthenticated, token } = useAdminStore();

  if (!isAuthenticated || !token) {
    return <Navigate to="/login" replace />;
  }

  return children;
};

const LoginRoute: React.FC = () => {
  const { isAuthenticated, token } = useAdminStore();

  if (isAuthenticated && token) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Login />;
};

const App: React.FC = () => {
  return (
    <ConfigProvider locale={zhCN}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginRoute />} />
          <Route
            path="/"
            element={
              <RequireAuth>
                <AdminLayout />
              </RequireAuth>
            }
          >
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="knowledge" element={<Knowledge />} />
            <Route path="avatar-studio" element={<AvatarStudio />} />
            <Route path="notifications" element={<NotificationComponent />} />
            <Route path="interactions" element={<Interactions />} />
            <Route path="statistics" element={<StatisticsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
};

export default App;
