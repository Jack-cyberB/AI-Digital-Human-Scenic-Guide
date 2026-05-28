import React, { useEffect, useState } from 'react';
import { Row, Col, Card, Statistic, Spin, Tag, Space } from 'antd';
import {
  UserOutlined,
  MessageOutlined,
  RiseOutlined,
  ClockCircleOutlined,
  ThunderboltOutlined,
  TeamOutlined,
  BgColorsOutlined,
} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import { wsClient } from '../../api/websocket';
import { statisticsApi } from '../../api';
import { useAdminStore } from '../../store';
import type { DashboardData } from '../../types';
import dayjs from 'dayjs';

const Dashboard: React.FC = () => {
  const { dashboardData, setDashboardData } = useAdminStore();
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
    initWebSocket();
  }, []);

  const fetchDashboardData = async () => {
    try {
      const response = await statisticsApi.getRealtime();
      if (response.data.code === 200) {
        setDashboardData(response.data.data);
      }
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const initWebSocket = () => {
    wsClient.connect();
    wsClient.on('DASHBOARD_UPDATE', (message) => {
      setDashboardData(message.payload as DashboardData);
    });
  };

  const getHourlyChartOption = () => {
    const hourlyData = dashboardData?.hourlyData || [];
    const hours = hourlyData.map((item) => `${item.hour}:00`);
    const counts = hourlyData.map((item) => item.count);

    return {
      grid: { left: 24, right: 24, top: 50, bottom: 24, containLabel: true },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: hours.length > 0 ? hours : Array.from({ length: 24 }, (_, i) => `${i}:00`),
        axisLine: { lineStyle: { color: '#cbd5e1' } },
      },
      yAxis: {
        type: 'value',
        name: '交互次数',
        splitLine: { lineStyle: { color: '#edf2f7' } },
      },
      series: [
        {
          name: '交互次数',
          type: 'line',
          smooth: true,
          symbolSize: 9,
          data: counts.length > 0 ? counts : [65, 45, 80, 120, 150, 200, 180, 220, 250, 280, 300, 320, 350, 380, 400, 420, 380, 350, 300, 250, 200, 150, 100, 80],
          areaStyle: { color: 'rgba(20, 184, 166, 0.18)' },
          lineStyle: { color: '#0f766e', width: 4 },
          itemStyle: { color: '#0f766e' },
        },
      ],
    };
  };

  const getPopularQAChartOption = () => {
    const popularQA = dashboardData?.popularQA || [];

    return {
      grid: { left: 20, right: 24, top: 18, bottom: 18, containLabel: true },
      tooltip: { trigger: 'item', formatter: '{b}: {c}次' },
      xAxis: { type: 'value', splitLine: { lineStyle: { color: '#edf2f7' } } },
      yAxis: {
        type: 'category',
        data: popularQA.map((item) => item.question.substring(0, 16) + '...'),
        inverse: true,
      },
      series: [
        {
          name: '查询次数',
          type: 'bar',
          data: popularQA.map((item) => item.count),
          itemStyle: { color: '#38bdf8', borderRadius: [0, 10, 10, 0] },
          label: { show: true, position: 'right' },
        },
      ],
    };
  };

  const getHotspotChartOption = () => {
    const hotspotSpots = dashboardData?.hotspotSpots || [
      { name: '主峰景区', count: 450 },
      { name: '湖泊观光区', count: 380 },
      { name: '森林氧吧', count: 320 },
      { name: '文化展览馆', count: 280 },
      { name: '游客中心', count: 200 },
    ];

    return {
      tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
      legend: { bottom: 0 },
      series: [
        {
          name: '访问人数',
          type: 'pie',
          radius: ['42%', '72%'],
          itemStyle: { borderRadius: 12, borderColor: '#fff', borderWidth: 2 },
          label: { show: true, formatter: '{b}: {c}' },
          data: hotspotSpots.map((item, index) => ({
            value: item.count,
            name: item.name,
            itemStyle: {
              color: ['#0f766e', '#14b8a6', '#38bdf8', '#8b5cf6', '#f59e0b'][index % 5],
            },
          })),
        },
      ],
    };
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 100 }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div className="dashboard-page">
      <section className="hero-panel">
        <div className="hero-content">
          <div className="hero-badge">智能景区运营平台</div>
          <h2>用现代化网站方式呈现景区运营数据</h2>
          <p>聚合 AI 数字人咨询、游客在线状态、热门景点热度和满意度反馈，帮助管理者快速掌握景区运行态势。</p>
          <div className="hero-metrics">
            <span>实时监控</span>
            <span>游客洞察</span>
            <span>应急联动</span>
          </div>
          <Space className="hero-mini-stats" size={12} wrap>
            <Tag color="green">在线服务正常</Tag>
            <Tag color="blue">知识库已同步</Tag>
            <Tag color="gold">今日高峰稳定</Tag>
          </Space>
        </div>
        <div className="hero-visual">
          <div className="orbit-card large">AI</div>
          <div className="orbit-card small top">导览</div>
          <div className="orbit-card small bottom">服务</div>
        </div>
      </section>

      <Row gutter={[20, 20]}>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} className="metric-card metric-green">
            <Statistic title="今日服务人次" value={dashboardData?.totalInteractions || 0} prefix={<UserOutlined />} valueStyle={{ fontWeight: 800 }} />
            <div className="metric-foot">较昨日保持活跃</div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} className="metric-card metric-blue">
            <Statistic title="当前在线游客" value={dashboardData?.onlineVisitors || 0} prefix={<ClockCircleOutlined />} valueStyle={{ fontWeight: 800 }} />
            <div className="metric-foot">在线服务通道正常</div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} className="metric-card metric-orange">
            <Statistic title="今日独立访客" value={dashboardData?.todayVisitors || 0} prefix={<TeamOutlined />} valueStyle={{ fontWeight: 800 }} />
            <div className="metric-foot">游客咨询持续增长</div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} className="metric-card metric-purple">
            <Statistic title="满意度" value={dashboardData?.satisfactionRate || 0} suffix="%" prefix={<RiseOutlined />} valueStyle={{ fontWeight: 800 }} />
            <div className="metric-foot">服务体验表现稳定</div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[20, 20]} className="content-grid">
        <Col xs={24} xl={14}>
          <Card bordered={false} className="modern-card" title="实时交互趋势">
            <ReactECharts option={getHourlyChartOption()} style={{ height: 340 }} />
          </Card>
        </Col>
        <Col xs={24} xl={10}>
          <Card bordered={false} className="modern-card" title="热门问答排行">
            <ReactECharts option={getPopularQAChartOption()} style={{ height: 340 }} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[20, 20]} className="content-grid">
        <Col xs={24} xl={10}>
          <Card bordered={false} className="modern-card" title="热门景点分布">
            <ReactECharts option={getHotspotChartOption()} style={{ height: 340 }} />
          </Card>
        </Col>
        <Col xs={24} xl={14}>
          <Card bordered={false} className="modern-card interactions-card" title="最近交互记录">
            <div className="interaction-list">
              {(dashboardData?.recentInteractions || []).map((item, index) => (
                <div key={index} className="interaction-item">
                  <div className="interaction-index">{String(index + 1).padStart(2, '0')}</div>
                  <div className="interaction-main">
                    <div className="interaction-question">{item.question}</div>
                    <div className="interaction-meta">
                      <span>{item.scenicSpot || '未指定景点'}</span>
                      <span>{dayjs(item.time).format('HH:mm:ss')}</span>
                    </div>
                  </div>
                </div>
              ))}
              {(!dashboardData?.recentInteractions || dashboardData.recentInteractions.length === 0) && (
                <div className="empty-state">暂无交互记录</div>
              )}
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
