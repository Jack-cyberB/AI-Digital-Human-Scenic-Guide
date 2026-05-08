import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Table, DatePicker, Space } from 'antd';
import {
  UserOutlined,
  MessageOutlined,
  RiseOutlined,
  CalendarOutlined,
  BarChartOutlined,
} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import { statisticsApi } from '../api';
import type { DailyStatistics } from '../types';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;

const StatisticsPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [historyData, setHistoryData] = useState<DailyStatistics[]>([]);
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  useEffect(() => {
    const defaultRange: [dayjs.Dayjs, dayjs.Dayjs] = [dayjs().subtract(7, 'day'), dayjs()];
    setDateRange(defaultRange);
    fetchHistoryData(defaultRange[0].format('YYYY-MM-DD'), defaultRange[1].format('YYYY-MM-DD'));
  }, []);

  const fetchHistoryData = async (startDate: string, endDate: string) => {
    setLoading(true);
    try {
      const response = await statisticsApi.getHistory(startDate, endDate);
      if (response.data.code === 200) {
        setHistoryData(response.data.data || []);
      }
    } catch (error) {
      console.error('Failed to fetch history data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDateChange = (dates: [dayjs.Dayjs, dayjs.Dayjs] | null) => {
    if (dates) {
      setDateRange(dates);
      fetchHistoryData(dates[0].format('YYYY-MM-DD'), dates[1].format('YYYY-MM-DD'));
    }
  };

  const getHistoryChartOption = () => {
    const dates = historyData.map((item) => dayjs(item.statDate).format('MM-DD'));
    const interactions = historyData.map((item) => item.totalInteractions || 0);
    const visitors = historyData.map((item) => item.totalVisitors || 0);

    return {
      grid: { left: 24, right: 24, top: 52, bottom: 24, containLabel: true },
      tooltip: { trigger: 'axis' },
      legend: { data: ['交互次数', '独立访客'], top: 18 },
      xAxis: { type: 'category', data: dates },
      yAxis: [
        { type: 'value', name: '交互次数' },
        { type: 'value', name: '独立访客' },
      ],
      series: [
        { name: '交互次数', type: 'bar', data: interactions, itemStyle: { color: '#0f766e', borderRadius: [8, 8, 0, 0] } },
        { name: '独立访客', type: 'line', yAxisIndex: 1, data: visitors, itemStyle: { color: '#38bdf8' }, smooth: true },
      ],
    };
  };

  const totalInteractions = historyData.reduce((sum, item) => sum + (item.totalInteractions || 0), 0);
  const totalVisitors = historyData.reduce((sum, item) => sum + (item.totalVisitors || 0), 0);
  const avgInteractions = historyData.length > 0 ? Math.round(totalInteractions / historyData.length) : 0;

  const columns = [
    { title: '日期', dataIndex: 'statDate', key: 'statDate', render: (date: string) => dayjs(date).format('YYYY-MM-DD') },
    { title: '交互次数', dataIndex: 'totalInteractions', key: 'totalInteractions' },
    { title: '独立访客', dataIndex: 'totalVisitors', key: 'totalVisitors' },
    { title: '高峰时段', dataIndex: 'peakHour', key: 'peakHour', render: (hour: number) => `${hour}:00 - ${hour + 1}:00` },
  ];

  return (
    <div className="page-card-shell">
      <Row gutter={[20, 20]} className="page-topbar">
        <Col xs={24} xl={16}>
          <Card bordered={false} className="modern-card">
            <Space direction="vertical" size={4}>
              <div className="eyebrow">Analytics Dashboard</div>
              <h2 className="section-title">统计报表</h2>
              <p className="section-description">通过历史数据分析运营趋势，为景区活动安排、服务排班和资源调度提供依据。</p>
            </Space>
          </Card>
        </Col>
        <Col xs={24} xl={8}>
          <Card bordered={false} className="modern-card summary-card">
            <CalendarOutlined />
            <strong>{dateRange ? `${dateRange[0].format('MM/DD')} - ${dateRange[1].format('MM/DD')}` : '--'}</strong>
            <span>当前统计区间</span>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={8}>
          <Card bordered={false} className="metric-card metric-green">
            <Statistic title="累计交互次数" value={totalInteractions} prefix={<MessageOutlined />} valueStyle={{ color: '#0f766e' }} />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card bordered={false} className="metric-card metric-blue">
            <Statistic title="累计独立访客" value={totalVisitors} prefix={<UserOutlined />} valueStyle={{ color: '#2563eb' }} />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card bordered={false} className="metric-card metric-orange">
            <Statistic title="日均交互次数" value={avgInteractions} prefix={<RiseOutlined />} valueStyle={{ color: '#f97316' }} />
          </Card>
        </Col>
      </Row>

      <Card bordered={false} className="modern-card toolbar-card" style={{ marginTop: 20 }}>
        <Space wrap>
          <BarChartOutlined />
          <RangePicker value={dateRange} onChange={handleDateChange} allowClear={false} />
        </Space>
      </Card>

      <Row gutter={[20, 20]} style={{ marginTop: 20 }}>
        <Col span={24}>
          <Card bordered={false} className="modern-card">
            <ReactECharts option={getHistoryChartOption()} style={{ height: 320 }} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[20, 20]} style={{ marginTop: 20 }}>
        <Col span={24}>
          <Card title="每日详细数据" bordered={false} className="modern-card">
            <Table columns={columns} dataSource={historyData} loading={loading} rowKey="id" pagination={false} />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default StatisticsPage;
