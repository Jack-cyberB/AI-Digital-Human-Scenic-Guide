import React, { useState } from 'react';
import { Form, Input, Button, message } from 'antd';
import {
  UserOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
  GlobalOutlined,
  BarChartOutlined,
  CloudServerOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { adminApi } from '../api';
import { useAdminStore } from '../store';

const Login: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { setToken, setUser, setAuthenticated } = useAdminStore();

  const handleSubmit = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const response = await adminApi.login(values);
      if (response.data.code === 200) {
        const { token, username, realName, role, loginTime } = response.data.data;
        setToken(token);
        setUser({ token, username, realName, role, loginTime });
        setAuthenticated(true);
        localStorage.setItem('admin_token', token);
        message.success('登录成功');
        navigate('/dashboard');
      } else {
        message.error(response.data.message || '登录失败');
      }
    } catch (error: any) {
      if (values.username === 'admin' && values.password === 'admin123') {
        const fallbackUser = {
          token: 'demo-admin-token',
          username: 'admin',
          realName: '系统管理员',
          role: 'SUPER_ADMIN',
          loginTime: new Date().toISOString(),
        };
        setToken(fallbackUser.token);
        setUser(fallbackUser);
        setAuthenticated(true);
        localStorage.setItem('admin_token', fallbackUser.token);
        message.warning('登录接口异常，已进入本地演示模式');
        navigate('/dashboard');
      } else {
        message.error(error.response?.data?.message || '登录失败，请检查用户名和密码');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-website">
      <section className="login-nav">
        <div className="login-brand">
          <span className="login-brand-mark"><GlobalOutlined /></span>
          <div>
            <strong>景区导览 AI</strong>
            <small>Smart Scenic Cloud</small>
          </div>
        </div>
        <div className="login-nav-links">
          <span>实时运营</span>
          <span>知识中台</span>
          <span>游客服务</span>
        </div>
      </section>

      <section className="login-hero">
        <div className="login-copy">
          <div className="login-pill"><SafetyCertificateOutlined /> 景区数字化管理平台</div>
          <h1>构建面向游客服务的现代化智慧景区网站</h1>
          <p>
            将 AI 导览、实时数据、热门景点、应急通知与运营报表整合到统一门户，
            让管理端既具备网站的品牌表达，也具备后台系统的高效操作体验。
          </p>
          <div className="login-feature-grid">
            <div className="login-feature-card">
              <BarChartOutlined />
              <strong>实时洞察</strong>
              <span>游客咨询与景点热度分钟级更新</span>
            </div>
            <div className="login-feature-card">
              <CloudServerOutlined />
              <strong>AI 服务中台</strong>
              <span>知识库、问答、通知一体化管理</span>
            </div>
          </div>
        </div>

        <div className="login-panel-wrap">
          <div className="login-panel">
            <div className="login-panel-header">
              <span>Admin Portal</span>
              <h2>欢迎回来</h2>
              <p>登录后进入智慧景区运营中心</p>
            </div>

            <Form name="login" onFinish={handleSubmit} size="large" layout="vertical" className="login-form">
              <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
                <Input prefix={<UserOutlined />} placeholder="请输入管理员账号" />
              </Form.Item>

              <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
                <Input.Password prefix={<LockOutlined />} placeholder="请输入登录密码" />
              </Form.Item>

              <Button type="primary" htmlType="submit" loading={loading} className="login-submit">
                进入运营中心
              </Button>
            </Form>

            <div className="login-demo-account">
              <span>演示账号</span>
              <strong>admin / admin123</strong>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
};

export default Login;
