import React, { useMemo, useState } from 'react';
import { Card, Col, Row, Select, Slider, Switch, Button, Tag, Space, Avatar, Divider, message } from 'antd';
import {
  RobotOutlined,
  SoundOutlined,
  PictureOutlined,
  BgColorsOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  SaveOutlined,
  GlobalOutlined,
} from '@ant-design/icons';

const { Option } = Select;

const skinOptions = [
  { id: 'forest-guide', name: '森林导游', theme: '绿意森系', color: 'from-emerald-500 to-teal-500' },
  { id: 'lake-guide', name: '湖畔讲解员', theme: '清透蓝调', color: 'from-sky-500 to-blue-500' },
  { id: 'heritage-guide', name: '文化讲述者', theme: '古典金棕', color: 'from-amber-500 to-orange-500' },
  { id: 'future-guide', name: '未来感数字人', theme: '科技银白', color: 'from-slate-500 to-cyan-500' },
];

const voiceOptions = [
  { id: 'female-warm', name: '温暖女声', style: '亲和细腻', sample: '适合导览、咨询与迎宾' },
  { id: 'male-deep', name: '稳重男声', style: '沉稳专业', sample: '适合讲解、播报与通知' },
  { id: 'youth-bright', name: '轻快青年音', style: '活力清新', sample: '适合互动问答和活动推荐' },
];

const AvatarStudio: React.FC = () => {
  const [skin, setSkin] = useState(skinOptions[0].id);
  const [voice, setVoice] = useState(voiceOptions[0].id);
  const [speed, setSpeed] = useState(48);
  const [pitch, setPitch] = useState(52);
  const [gesture, setGesture] = useState(true);

  const selectedSkin = useMemo(() => skinOptions.find((item) => item.id === skin) || skinOptions[0], [skin]);
  const selectedVoice = useMemo(() => voiceOptions.find((item) => item.id === voice) || voiceOptions[0], [voice]);

  const handleSave = () => {
    message.success('数字人配置已保存（当前为前端展示配置，可继续对接后端接口）');
  };

  return (
    <div className="page-card-shell">
      <Row gutter={[20, 20]} className="page-topbar">
        <Col xs={24} xl={16}>
          <Card bordered={false} className="modern-card">
            <Space direction="vertical" size={4}>
              <div className="eyebrow">Digital Human Studio</div>
              <h2 className="section-title">数字人皮肤与声音管理</h2>
              <p className="section-description">
                根据大赛文档，管理员需要能够配置数字人的外观、声音、语速与互动风格，以匹配不同景区文化气质。
              </p>
            </Space>
          </Card>
        </Col>
        <Col xs={24} xl={8}>
          <Card bordered={false} className="modern-card summary-card">
            <RobotOutlined />
            <strong>4 + 3</strong>
            <span>皮肤 / 声音预设</span>
          </Card>
        </Col>
      </Row>

      <Row gutter={[20, 20]}>
        <Col xs={24} xl={10}>
          <Card bordered={false} className="modern-card" title="数字人预览">
            <div className="avatar-preview-card">
              <div className={`avatar-preview-surface ${selectedSkin.color}`}>
                <Avatar size={120} className="avatar-preview">A</Avatar>
                <Tag color="processing" className="avatar-preview-tag">{selectedSkin.theme}</Tag>
              </div>
              <div className="avatar-preview-meta">
                <h3>{selectedSkin.name}</h3>
                <p>当前声音：{selectedVoice.name}</p>
                <span>{selectedVoice.style} · 语速 {speed}% · 音调 {pitch}%</span>
              </div>
            </div>
            <Divider />
            <div className="studio-feature-list">
              <div><EyeOutlined /> 实时预览形象效果</div>
              <div><SoundOutlined /> 支持多音色切换</div>
              <div><GlobalOutlined /> 可匹配景区文化主题</div>
            </div>
          </Card>
        </Col>

        <Col xs={24} xl={14}>
          <Card bordered={false} className="modern-card" title="皮肤与声音配置">
            <Row gutter={[16, 16]}>
              <Col xs={24} md={12}>
                <div className="form-block-title"><PictureOutlined /> 皮肤选择</div>
                <div className="studio-option-grid">
                  {skinOptions.map((item) => (
                    <button key={item.id} className={`studio-option ${skin === item.id ? 'active' : ''}`} onClick={() => setSkin(item.id)}>
                      <strong>{item.name}</strong>
                      <span>{item.theme}</span>
                    </button>
                  ))}
                </div>
              </Col>
              <Col xs={24} md={12}>
                <div className="form-block-title"><SoundOutlined /> 声音选择</div>
                <Select value={voice} onChange={setVoice} style={{ width: '100%' }} size="large">
                  {voiceOptions.map((item) => <Option key={item.id} value={item.id}>{item.name}</Option>)}
                </Select>
                <div className="voice-sample-card">
                  <strong>{selectedVoice.name}</strong>
                  <span>{selectedVoice.sample}</span>
                  <Button type="primary" ghost icon={<PlayCircleOutlined />}>试听示例</Button>
                </div>
              </Col>
            </Row>

            <Row gutter={[16, 16]} style={{ marginTop: 12 }}>
              <Col xs={24} md={12}>
                <div className="form-block-title"><BgColorsOutlined /> 语速</div>
                <Slider value={speed} onChange={setSpeed} min={20} max={80} />
              </Col>
              <Col xs={24} md={12}>
                <div className="form-block-title"><BgColorsOutlined /> 音调</div>
                <Slider value={pitch} onChange={setPitch} min={20} max={80} />
              </Col>
            </Row>

            <div className="studio-switch-row">
              <span>开启口型与手势同步</span>
              <Switch checked={gesture} onChange={setGesture} />
            </div>

            <div className="studio-save-row">
              <Button icon={<SaveOutlined />} onClick={handleSave} type="primary">保存数字人配置</Button>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default AvatarStudio;
