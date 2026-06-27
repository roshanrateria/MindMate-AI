// ==================== Domain Types ====================

export interface CheckIn {
  id?: number;
  timestamp: number;
  mood: number;          // 0-10
  stress: number;        // 0-10
  anxiety: number;       // 0-10
  energy: number;        // 0-10
  motivation: number;    // 0-10
  confidence: number;    // 0-10
  sleepHours: number;
  sleepQuality: 'Restful' | 'Disturbed' | "Couldn't sleep";
  studyHours: number;
  studyBreaks: boolean;
  mealsEaten: 'Yes' | 'Skipped some' | 'Barely ate';
  triggers: string[];
  journalText: string;
}

export type BurnoutLevel = 'RESILIENT' | 'CAUTION' | 'AT_RISK' | 'CRISIS_ALERT';

export interface AIAnalysis {
  id?: number;
  checkInId: number;
  timestamp: number;
  primaryEmotion: string;
  detectedTriggers: string[];
  intrapersonalScore: number;  // 0-33
  interpersonalScore: number;  // 0-33
  academicScore: number;       // 0-33
  totalBurnoutScore: number;   // 0-100
  burnoutLevel: BurnoutLevel;
  suggestion: string;
  companionMessage: string;
}

export interface ChatMessage {
  id?: number;
  timestamp: number;
  sender: 'user' | 'ai';
  message: string;
}

export type CrisisLevel = 'NONE' | 'MEDIUM' | 'HIGH';

export interface CrisisResult {
  isCrisis: boolean;
  level: CrisisLevel;
  matchedKeywords: string[];
}

export interface OnboardingData {
  nickname: string;
  examType: string;
  biggestWorry: string;
  completed: boolean;
}

export interface DashboardState {
  recentCheckIns: CheckIn[];
  overallWellnessScore: number;
  burnoutScore: number;
  burnoutLevel: BurnoutLevel;
  latestAnalysis: AIAnalysis | null;
  streakCount: number;
  intrapersonalBurnout: number;
  interpersonalBurnout: number;
  academicBurnout: number;
  triggerCounts: Record<string, number>;
  hasCrisisScore: boolean;
}
