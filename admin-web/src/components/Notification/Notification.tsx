import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Space,
  Select,
  Modal,
  Form,
  Input,
  message,
  Popconfirm,
  Tag,
  Card,
  Row,
  Col,
} from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  SendOutlined,
  BellOutlined,
} from '@ant-design/icons';
import { notificationApi } from '../../api';
import { wsClient } from '../../api/websocket';
import type { Notification } from '../../types';
import dayjs from 'dayjs';

const { TextArea } = Input;
const { Option } = Select;

const NotificationComponent: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<Notification[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchData();
  }, [pagination.current, pagination.pageSize]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const response = await notificationApi.getList({ pageNum: pagination.current, pageSize: pagination.pageSize });
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
    form.resetFields();
    setModalVisible(true);
  };

  const handleSend = async (record: Notification) => {
    wsClient.sendNotification({
      title: record.title,
      content: record.content,
      notificationType: record.notificationType,
      targetScope: record.targetScope,
      targetSpot: record.targetSpot,
    });
    message.success('通知已推送');
  };

  const handleDelete = async (id: number) => {
    try {
      const response = await notificationApi.delete(id);
      if (response.data.code === 200) {
        message.success('删除成功');
        fetchData();
      }
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const response = await notificationApi.create(values);
      if (response.data.code === 200) {
        message.success('创建成功');
        wsClient.sendNotification({
          title: values.title,
          content: values.content,
          notificationType: values.notificationType,
          targetScope: values.targetScope,
          targetSpot: values.targetSpot,
        });
        message.success('通知已推送');
        setModalVisible(false);
        fetchData();
      }
    } catch (error) {
      message.error('操作失败');
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
    { title: '内容', dataIndex: 'content', key: 'content', ellipsis: true },
    {
      title: '类型', dataIndex: 'notificationType', key: 'notificationType', width: 120,
      render: (type: string) => {
        const colorMap: Record<string, string> = { EMERGENCY: 'red', INFO: 'blue', UPDATE: 'green' };
        return <Tag color={colorMap[type] || 'default'}>{type}</Tag>;
      },
    },
    {
      title: '范围', dataIndex: 'targetScope', key: 'targetScope', width: 100,
      render: (scope: string) => ({ ALL: '全部', SCENIC_AREA: '景区', SPOT: '指定景点' }[scope] || scope),
    },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 100,
      render: (status: number) => {
        const statusMap: Record<number, { text: string; color: string }> = { 0: { text: '待推送', color: 'default' }, 1: { text: '已推送', color: 'success' }, 2: { text: '已过期', color: 'warning' } };
        const { text, color } = statusMap[status] || { text: '未知', color: 'default' };
        return <Tag color={color}>{text}</Tag>;
      },
    },
    { title: '已读', dataIndex: 'readCount', key: 'readCount', width: 80 },
    { title: '推送时间', dataIndex: 'pushTime', key: 'pushTime', width: 180, render: (time: string) => (time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-') },
    {
      title: '操作', key: 'action', width: 170,
      render: (_: any, record: Notification) => (
        <Space>
          {record.status !== 1 && <Button type="primary" size="small" icon={<SendOutlined />} onClick={() => handleSend(record)}>推送</Button>}
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
              <div className="eyebrow">Notification Center</div>
              <h2 className="section-title">紧急通知发布</h2>
              <p className="section-description">统一管理景区通知与紧急事件推送，支持一键同步到游客端。</p>
            </Space>
          </Card>
        </Col>
        <Col xs={24} xl={8}>
          <Card bordered={false} className="modern-card summary-card">
            <BellOutlined />
            <strong>{data.length}</strong>
            <span>当前页通知数量</span>
          </Card>
        </Col>
      </Row>

      <Card bordered={false} className="modern-card toolbar-card">
        <Space className="toolbar-row">
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>创建通知</Button>
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

      <Modal title="创建紧急通知" open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)} width={600}>
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="通知标题" rules={[{ required: true, message: '请输入通知标题' }]}><Input placeholder="请输入通知标题" maxLength={100} /></Form.Item>
          <Form.Item name="content" label="通知内容" rules={[{ required: true, message: '请输入通知内容' }]}><TextArea rows={4} placeholder="请输入通知内容" maxLength={500} /></Form.Item>
          <Space wrap>
            <Form.Item name="notificationType" label="通知类型" initialValue="INFO">
              <Select style={{ width: 150 }}>
                <Option value="INFO">普通通知</Option>
                <Option value="EMERGENCY">紧急通知</Option>
                <Option value="UPDATE">更新通知</Option>
              </Select>
            </Form.Item>
            <Form.Item name="targetScope" label="推送范围" initialValue="ALL">
              <Select style={{ width: 150 }}>
                <Option value="ALL">全部游客</Option>
                <Option value="SCENIC_AREA">景区范围</Option>
                <Option value="SPOT">指定景点</Option>
              </Select>
            </Form.Item>
          </Space>
          <Form.Item name="targetSpot" label="指定景点"><Input placeholder="当范围为指定景点时填写" /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default NotificationComponent;
