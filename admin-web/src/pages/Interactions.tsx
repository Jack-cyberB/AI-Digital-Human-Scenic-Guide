import React, { useEffect, useState } from 'react';
import { Table, Card, Tag, Space, Input, Row, Col } from 'antd';
import { SearchOutlined, HistoryOutlined } from '@ant-design/icons';
import { statisticsApi } from '../api';
import type { RecentInteraction } from '../types';
import dayjs from 'dayjs';

const { Search } = Input;

const Interactions: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<RecentInteraction[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });
  const [searchKeyword, setSearchKeyword] = useState('');

  useEffect(() => {
    fetchData();
  }, [pagination.current, pagination.pageSize]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const response = await statisticsApi.getInteractions({ page: pagination.current, size: pagination.pageSize });
      if (response.data.code === 200) {
        setData(response.data.data.records || []);
        setPagination((prev) => ({ ...prev, total: response.data.data.total || 0 }));
      }
    } catch (error) {
      console.error('Failed to fetch interactions:', error);
    } finally {
      setLoading(false);
    }
  };

  const filteredData = searchKeyword
    ? data.filter((item) => item.question.includes(searchKeyword) || item.answer.includes(searchKeyword) || item.visitorId.includes(searchKeyword))
    : data;

  const columns = [
    {
      title: '访客ID', dataIndex: 'visitorId', key: 'visitorId', width: 180, ellipsis: true,
      render: (id: string) => <Tag color="blue">{id.substring(0, 16)}...</Tag>,
    },
    { title: '问题', dataIndex: 'question', key: 'question', ellipsis: true },
    { title: '回答', dataIndex: 'answer', key: 'answer', ellipsis: true },
    {
      title: '景点', dataIndex: 'scenicSpot', key: 'scenicSpot', width: 120,
      render: (spot: string) => (spot ? <Tag color="green">{spot}</Tag> : '-'),
    },
    { title: '时间', dataIndex: 'time', key: 'time', width: 180, render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm:ss') },
  ];

  return (
    <div className="page-card-shell">
      <Row gutter={[20, 20]} className="page-topbar">
        <Col xs={24} xl={16}>
          <Card bordered={false} className="modern-card">
            <Space direction="vertical" size={4}>
              <div className="eyebrow">Interaction Timeline</div>
              <h2 className="section-title">交互记录</h2>
              <p className="section-description">查看游客与 AI 的历史咨询内容，便于持续优化问答知识与服务策略。</p>
            </Space>
          </Card>
        </Col>
        <Col xs={24} xl={8}>
          <Card bordered={false} className="modern-card summary-card">
            <HistoryOutlined />
            <strong>{filteredData.length}</strong>
            <span>当前筛选结果</span>
          </Card>
        </Col>
      </Row>

      <Card bordered={false} className="modern-card toolbar-card">
        <Search placeholder="搜索问题/回答/访客ID" onSearch={(value) => setSearchKeyword(value)} style={{ width: 320 }} allowClear prefix={<SearchOutlined />} />
      </Card>

      <Card bordered={false} className="modern-card table-card">
        <Table
          columns={columns}
          dataSource={filteredData}
          loading={loading}
          rowKey={(record) => `${record.visitorId}-${record.time}`}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => setPagination({ current: page, pageSize, total: pagination.total }),
          }}
        />
      </Card>
    </div>
  );
};

export default Interactions;
