USE jingqu_db;

INSERT INTO admins (username, password, real_name, role, status)
VALUES (
    'admin',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    'System Admin',
    'SUPER_ADMIN',
    1
)
ON DUPLICATE KEY UPDATE username = username;

INSERT INTO knowledge_base (question_pattern, answer, keywords, category, priority, status)
VALUES
(
    '开放时间|几点开门|营业时间',
    '景区开放时间以官方公告和现场公示为准，建议您在出行前通过景区官方渠道再次确认。',
    '开放时间,开门,营业',
    'GENERAL',
    10,
    1
),
(
    '门票|票价|多少钱',
    '门票及优惠政策请以景区官方公告为准，您也可以在景区售票渠道或游客服务中心查询最新信息。',
    '门票,票价,价格,优惠',
    'GENERAL',
    10,
    1
),
(
    '停车|停车场',
    '如果您需要停车信息，我可以先为您提供一般指引；更准确的位置和空位情况建议查看现场导视或咨询服务人员。',
    '停车,停车场,车位',
    'GENERAL',
    8,
    1
),
(
    '厕所|洗手间|卫生间|wc',
    '景区内通常会在主要游览节点、游客中心和餐饮区域设置洗手间，建议您查看最近的现场导视标识。',
    '厕所,洗手间,卫生间,WC',
    'GENERAL',
    8,
    1
),
(
    '客服|热线|求助|投诉',
    '如需人工帮助，建议您前往游客服务中心或联系景区官方客服渠道。',
    '客服,热线,求助,投诉',
    'GENERAL',
    9,
    1
),
(
    '天气|今天天气',
    '天气属于实时信息，建议您查看手机天气服务或景区现场公告，以获取最新情况。',
    '天气,实时天气',
    'GENERAL',
    7,
    1
)
ON DUPLICATE KEY UPDATE question_pattern = question_pattern;
