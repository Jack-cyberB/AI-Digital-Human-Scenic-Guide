export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  realName: string;
  role: string;
  loginTime: string;
}

export interface DashboardData {
  totalInteractions: number;
  onlineVisitors: number;
  todayVisitors: number;
  satisfactionRate: number;
  popularQA: PopularQA[];
  hotspotSpots: HotspotSpot[];
  hourlyData: HourlyData[];
  recentInteractions: RecentInteraction[];
  updateTime: string;
}

export interface PopularQA {
  question: string;
  answer: string;
  count: number;
}

export interface HotspotSpot {
  name: string;
  count: number;
}

export interface HourlyData {
  hour: number;
  count: number;
}

export interface RecentInteraction {
  visitorId: string;
  question: string;
  answer: string;
  scenicSpot: string;
  finalAnswerSource?: string;
  fallbackUsed?: boolean;
  time: string;
}

export interface DailyStatistics {
  id?: number;
  statDate: string;
  totalInteractions: number;
  totalVisitors: number;
  peakHour: number;
}

export interface KnowledgeItem {
  id?: number;
  questionPattern: string;
  answer: string;
  keywords: string;
  category: string;
  priority: number;
  status: number;
  viewCount?: number;
  successCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface Notification {
  id?: number;
  title: string;
  content: string;
  notificationType: string;
  targetScope: string;
  targetSpot?: string;
  pushTime?: string;
  expiryTime?: string;
  status?: number;
  readCount?: number;
  createdBy?: string;
  createdAt?: string;
}

export interface WebSocketMessage {
  type: string;
  target?: string;
  payload: any;
  timestamp?: string;
}
