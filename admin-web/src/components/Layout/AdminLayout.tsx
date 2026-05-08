import React, { useState } from 'react';
import { Layout, Avatar, Dropdown, Badge, Button, Space, Typography, Drawer, List, Tag, Divider } from 'antd';
import {
  DashboardOutlined,
  BookOutlined,
  NotificationOutlined,
  HistoryOutlined,
  BarChartOutlined,
  LogoutOutlined,
  UserOutlined,
  BellOutlined,
  GlobalOutlined,
  CustomerServiceOutlined,
  MenuOutlined,
  ArrowRightOutlined,
  RobotOutlined,
} from '@ant-design/icons';
import { useNavigate, useLocation, Outlet } from 'react-router-dom';
import { useAdminStore } from '../../store';

const { Header, Content } = Layout;
const { Text } = Typography;

const pageMeta: Record<string, { title: string; subtitle: string }> = {
  '/dashboard': { title: '智慧景区运营中心', subtitle: '面向游客服务、景点热度、应急通知与 AI 数字人的一站式管理平台' },
  '/knowledge': { title: '知识库管理', subtitle: '结构化沉淀景点、路线、服务与应急问答，让 AI 服务更准确' },
  '/avatar-studio': { title: '数字人皮肤与声音', subtitle: '配置数字人的外观、服饰、声线与交互风格，匹配不同景区场景' },
  '/notifications': { title: '紧急通知', subtitle: '统一发布景区突发事件、广播提醒与游客安全提示' },
  '/interactions': { title: '交互记录', subtitle: '追踪游客咨询内容，持续优化数字人服务体验' },
  '/statistics': { title: '统计报表', subtitle: '以清晰的数据分析辅助景区运营和资源调度决策' },
};

const AdminLayout: React.FC = () => {
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAdminStore();
  const currentPage = pageMeta[location.pathname] || pageMeta['/dashboard'];

  const navItems = [
    { key: '/dashboard', icon: <DashboardOutlined />, label: '首页总览' },
    { key: '/knowledge', icon: <BookOutlined />, label: '知识库' },
    { key: '/avatar-studio', icon: <RobotOutlined />, label: '数字人配置' },
    { key: '/notifications', icon: <NotificationOutlined />, label: '通知发布' },
    { key: '/interactions', icon: <HistoryOutlined />, label: '交互记录' },
    { key: '/statistics', icon: <BarChartOutlined />, label: '数据报表' },
  ];

  const userMenuItems = [
    { key: 'profile', icon: <UserOutlined />, label: '个人信息' },
    { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', danger: true },
  ];

  const handleUserMenuClick = (e: { key: string }) => {
    if (e.key === 'logout') {
      localStorage.removeItem('admin_token');
      localStorage.setItem('admin_skip_auto_login_once', '1');
      logout();
      navigate('/login');
    }
  };

  return (
    <Layout className="site-shell">
      <Header className="website-header">
        <div className="website-header-inner">
          <button className="mobile-menu" aria-label="打开导航" onClick={() => setMenuOpen(true)}>
            <MenuOutlined />
          </button>
          <div className="website-brand" onClick={() => navigate('/dashboard')}>
            <div className="brand-logo">
              <GlobalOutlined />
            </div>
            <div>
              <div className="brand-title">景区导览</div>
              <div className="brand-subtitle">AI Scenic Platform</div>
            </div>
          </div>

          <nav className="website-nav" aria-label="主导航">
            {navItems.map((item) => (
              <button key={item.key} className={`nav-link ${location.pathname === item.key ? 'active' : ''}`} onClick={() => navigate(item.key)}>
                {item.icon}
                <span>{item.label}</span>
              </button>
            ))}
          </nav>

          <div className="header-actions">
            <Button className="quick-action" type="primary" icon={<CustomerServiceOutlined />} onClick={() => navigate('/avatar-studio')}>
              服务中台
            </Button>
            <Button className="notification-trigger" aria-label="通知中心" onClick={() => navigate('/notifications')}>
              <Badge count={5} size="small">
                <BellOutlined />
              </Badge>
            </Button>
            <Dropdown menu={{ items: userMenuItems, onClick: handleUserMenuClick }} placement="bottomRight">
              <div className="user-profile">
                <Avatar className="user-avatar">{user?.realName?.charAt(0) || 'A'}</Avatar>
                <div className="user-info">
                  <span>{user?.realName || '管理员'}</span>
                  <small>景区运营管理员</small>
                </div>
              </div>
            </Dropdown>
          </div>
        </div>
      </Header>

      <Content className="site-content">
        <section className="page-title-section">
          <div>
            <div className="eyebrow">Smart Tourism Website</div>
            <h1>{currentPage.title}</h1>
            <p>{currentPage.subtitle}</p>
            <Space className="page-breadcrumb" size={10}>
              <Text type="secondary">现代化网站布局</Text>
              <ArrowRightOutlined />
              <Text strong>{currentPage.title}</Text>
            </Space>
          </div>
          <div className="title-stats">
            <span>实时在线</span>
            <strong>7 × 24</strong>
          </div>
        </section>
        <Outlet />
      </Content>

      <Drawer title="主导航" open={menuOpen} onClose={() => setMenuOpen(false)} placement="left" width={280}>
        <List
          dataSource={navItems}
          renderItem={(item) => (
            <List.Item onClick={() => { navigate(item.key); setMenuOpen(false); }} style={{ cursor: 'pointer' }}>
              <Space>
                {item.icon}
                <span>{item.label}</span>
              </Space>
            </List.Item>
          )}
        />
        <Divider />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Tag color="green">服务中台已启用</Tag>
          <Tag color="blue">通知中心可直达</Tag>
        </div>
      </Drawer>
    </Layout>
  );
};

export default AdminLayout;
