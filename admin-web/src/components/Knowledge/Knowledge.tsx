import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Space,
  Input,
  Select,
  Modal,
  Form,
  message,
  Popconfirm,
  Tag,
  Card,
  Row,
  Col,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  SyncOutlined,
  BookOutlined,
} from '@ant-design/icons';
import { knowledgeApi } from '../../api';
import { wsClient } from '../../api/websocket';
import type { KnowledgeItem } from '../../types';

const { Option } = Select;

const Knowledge: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<KnowledgeItem[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [searchParams, setSearchParams] = useState({ category: '', status: undefined as number | undefined });
  const [modalVisible, setModalVisible] = useState(false);
  const [editingItem, setEditingItem] = useState<KnowledgeItem | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchData();
  }, [pagination.current, pagination.pageSize, searchParams]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const response = await knowledgeApi.getList({
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        category: searchParams.category || undefined,
        status: searchParams.status,
      });
      if (response.data.code === 200) {
        setData(response.data.data.records);
        setPagination((prev) => ({ ...prev, total: response.data.data.total }));
      }
    } catch (error) {
      message.error('获取数据失败');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = () => {
    setEditingItem(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record: KnowledgeItem) => {
    setEditingItem(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      const response = await knowledgeApi.delete(id);
      if (response.data.code === 200) {
        message.success('删除成功');
        fetchData();
      }
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleSync = () => {
    wsClient.syncKnowledge('full_sync', null);
    message.success('已同步知识库到游客端');
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingItem?.id) {
        const response = await knowledgeApi.update(editingItem.id, values);
        if (response.data.code === 200) {
          message.success('更新成功');
        }
      } else {
        const response = await knowledgeApi.create(values);
        if (response.data.code === 200) {
          message.success('添加成功');
        }
      }
      setModalVisible(false);
      fetchData();
    } catch (error) {
      message.error('操作失败');
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: '问题模式', dataIndex: 'questionPattern', key: 'questionPattern', ellipsis: true },
    { title: '答案', dataIndex: 'answer', key: 'answer', ellipsis: true },
    {
      title: '分类', dataIndex: 'category', key: 'category', width: 120,
      render: (category: string) => {
        const colorMap: Record<string, string> = { '景点介绍': 'green', '路线规划': 'blue', '餐饮服务': 'orange', '紧急求助': 'red', GENERAL: 'default' };
        return <Tag color={colorMap[category] || 'default'}>{category}</Tag>;
      },
    },
    { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80 },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 80,
      render: (status: number) => <Tag color={status === 1 ? 'success' : 'default'}>{status === 1 ? '启用' : '禁用'}</Tag>,
    },
    { title: '查询次数', dataIndex: 'viewCount', key: 'viewCount', width: 100 },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 180 },
    {
      title: '操作', key: 'action', width: 150,
      render: (_: any, record: KnowledgeItem) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm title="确认删除？" onConfirm={() => handleDelete(record.id!)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="page-card-shell">
      <Row gutter={[20, 20]} className="page-topbar">
        <Col xs={24} xl={16}>
          <Card bordered={false} className="modern-card">
            <Space direction="vertical" size={4}>
              <div className="eyebrow">Knowledge Center</div>
              <h2 className="section-title">知识库管理</h2>
              <p className="section-description">统一维护景点、路线、服务与应急问答，提升游客端 AI 回复准确率。</p>
            </Space>
          </Card>
        </Col>
        <Col xs={24} xl={8}>
          <Card bordered={false} className="modern-card summary-card">
            <BookOutlined />
            <strong>{data.length}</strong>
            <span>当前页知识条目</span>
          </Card>
        </Col>
      </Row>

      <Card bordered={false} className="modern-card toolbar-card">
        <Space wrap className="toolbar-row">
          <Input placeholder="搜索分类" value={searchParams.category} onChange={(e) => setSearchParams((prev) => ({ ...prev, category: e.target.value }))} prefix={<SearchOutlined />} style={{ width: 220 }} />
          <Select placeholder="状态筛选" allowClear value={searchParams.status} onChange={(value) => setSearchParams((prev) => ({ ...prev, status: value }))} style={{ width: 140 }}>
            <Option value={1}>启用</Option>
            <Option value={0}>禁用</Option>
          </Select>
          <Button icon={<SyncOutlined />} onClick={handleSync}>同步知识库</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>添加知识</Button>
        </Space>
      </Card>

      <Card bordered={false} className="modern-card table-card">
        <Table
          columns={columns}
          dataSource={data}
          loading={loading}
          rowKey="id"
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

      <Modal title={editingItem ? '编辑知识' : '添加知识'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)} width={700}>
        <Form form={form} layout="vertical">
          <Form.Item name="questionPattern" label="问题模式" rules={[{ required: true, message: '请输入问题模式' }]}>
            <Input.TextArea rows={2} placeholder="支持多个问题，用 | 或 ，分隔" />
          </Form.Item>
          <Form.Item name="answer" label="标准答案" rules={[{ required: true, message: '请输入标准答案' }]}>
            <Input.TextArea rows={4} placeholder="请输入标准回答内容" />
          </Form.Item>
          <Form.Item name="keywords" label="关键词">
            <Input placeholder="用逗号分隔的关键词" />
          </Form.Item>
          <Space wrap>
            <Form.Item name="category" label="分类" initialValue="GENERAL">
              <Select style={{ width: 150 }}>
                <Option value="景点介绍">景点介绍</Option>
                <Option value="路线规划">路线规划</Option>
                <Option value="餐饮服务">餐饮服务</Option>
                <Option value="紧急求助">紧急求助</Option>
                <Option value="GENERAL">通用</Option>
              </Select>
            </Form.Item>
            <Form.Item name="priority" label="优先级" initialValue={0}>
              <Input type="number" style={{ width: 100 }} placeholder="0" />
            </Form.Item>
            <Form.Item name="status" label="状态" initialValue={1}>
              <Select style={{ width: 100 }}>
                <Option value={1}>启用</Option>
                <Option value={0}>禁用</Option>
              </Select>
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
};

export default Knowledge;
