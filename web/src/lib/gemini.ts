import type { CheckIn, AIAnalysis, BurnoutLevel, CrisisResult, CrisisLevel, ChatMessage } from '../types';

const GEMINI_API_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent';

function getApiKey(): string {
  return localStorage.getItem('mindmate_gemini_key') || import.meta.env.VITE_GEMINI_API_KEY || '';
}

export function setApiKey(key: string) {
  localStorage.setItem('mindmate_gemini_key', key);
}

export function hasApiKey(): boolean {
  return !!getApiKey();
}

// ==================== Crisis Detection ====================

const CRISIS_KEYWORDS = [
  'no point', 'worthless', "can't do this", 'end it', 'give up on life',
  'disappear', 'kill myself', 'suicide', 'better off dead', 'want to die',
  'ending my life', 'no hope', 'cannot go on', 'sleeping forever',
  'nothing matters anymore', 'everyone hates me', 'disappoint my parents',
  'fail my family', 'worthless student'
];

const HIGH_CRISIS = ['kill myself', 'suicide', 'better off dead', 'want to die', 'end it'];

export function detectCrisis(text: string): CrisisResult {
  if (!text.trim()) return { isCrisis: false, level: 'NONE', matchedKeywords: [] };
  const lower = text.toLowerCase();
  const matched = CRISIS_KEYWORDS.filter(kw => lower.includes(kw));
  if (matched.length === 0) return { isCrisis: false, level: 'NONE', matchedKeywords: [] };

  const level: CrisisLevel = matched.some(m => HIGH_CRISIS.includes(m)) ? 'HIGH' : 'MEDIUM';
  return { isCrisis: true, level, matchedKeywords: matched };
}

// ==================== Wellness Score ====================

export function getWellnessScore(checkIn: CheckIn): number {
  const moodPoints = checkIn.mood * 10;
  const sleepQualityPoints = checkIn.sleepQuality === 'Restful' ? 100 : checkIn.sleepQuality === 'Disturbed' ? 50 : 0;
  const sleepDurationPoints = (checkIn.sleepHours >= 7 && checkIn.sleepHours <= 9) ? 100 : checkIn.sleepHours >= 5 ? 70 : checkIn.sleepHours > 9 ? 60 : 30;
  const sleepPoints = (sleepQualityPoints * 0.5) + (sleepDurationPoints * 0.5);
  const stressPoints = (10 - checkIn.stress) * 10;
  const breakPoints = checkIn.studyBreaks ? 100 : 0;
  const mealPoints = checkIn.mealsEaten === 'Yes' ? 100 : checkIn.mealsEaten === 'Skipped some' ? 50 : 10;
  const confidencePoints = checkIn.confidence * 10;
  const lifestylePoints = (mealPoints * 0.5) + (confidencePoints * 0.5);

  const finalScore = (moodPoints * 0.25) + (sleepPoints * 0.20) + (stressPoints * 0.25) + (breakPoints * 0.15) + (lifestylePoints * 0.15);
  return Math.round(Math.max(0, Math.min(100, finalScore)));
}

// ==================== Burnout Score ====================

export function getBurnoutLevel(total: number): BurnoutLevel {
  if (total >= 76) return 'CRISIS_ALERT';
  if (total >= 56) return 'AT_RISK';
  if (total >= 31) return 'CAUTION';
  return 'RESILIENT';
}

// ==================== Streak Calculation ====================

export function calculateStreak(checkIns: CheckIn[]): number {
  if (checkIns.length === 0) return 0;
  let streak = 1;
  const oneDayMs = 24 * 60 * 60 * 1000;
  for (let i = 0; i < checkIns.length - 1; i++) {
    const diff = checkIns[i].timestamp - checkIns[i + 1].timestamp;
    if (diff <= oneDayMs * 1.5) streak++;
    else break;
  }
  return streak;
}

// ==================== Offline Fallback Analysis ====================

function generateOfflineAnalysis(checkIn: CheckIn): AIAnalysis {
  const crisisResult = detectCrisis(checkIn.journalText);
  const detectedTriggers: string[] = [];
  if (checkIn.triggers.length > 0) {
    detectedTriggers.push(...checkIn.triggers.map(t => t.replace(/^[^\s]+\s/, '')));
  } else {
    const text = checkIn.journalText.toLowerCase();
    if (text.includes('parents') || text.includes('expectations')) detectedTriggers.push('Parental Expectations');
    if (text.includes('mock') || text.includes('test') || text.includes('score')) detectedTriggers.push('Mock Test Scores');
    if (text.includes('backlog') || text.includes('study')) detectedTriggers.push('Cognitive Overload');
    if (text.includes('alone') || text.includes('lonely')) detectedTriggers.push('Social Isolation');
  }
  if (detectedTriggers.length === 0) detectedTriggers.push('General Exam Tension');

  const intra = Math.min(33, Math.max(5, Math.round((10 - checkIn.energy) * 3.3 + (10 - checkIn.confidence) * 1.5 + (checkIn.sleepQuality !== 'Restful' ? 15 : 0))));
  const inter = Math.min(33, Math.max(5, Math.round(checkIn.stress * 2.5 + 5)));
  const academic = Math.min(33, Math.max(5, Math.round((10 - checkIn.motivation) * 2.5 + (!checkIn.studyBreaks ? 12 : 0) + (checkIn.studyHours > 12 ? 10 : 5))));
  const totalBurnout = Math.min(100, intra + inter + academic);
  const burnoutLevel = getBurnoutLevel(totalBurnout);

  const suggestion = totalBurnout >= 76
    ? 'Please take a complete break today. Step away from your study desk, and speak to a trusted friend or counselor.'
    : checkIn.stress >= 7
    ? `You logged ${checkIn.studyHours} hours of study, but your stress is high. Try the 50-10 Pomodoro routine.`
    : checkIn.sleepHours < 6
    ? `Your sleep (${checkIn.sleepHours}h) is below the threshold for memory consolidation. Prioritize 7+ hours tonight.`
    : 'Your routine is structured well today. Ensure you dedicate 30 minutes to winding down before bed.';

  const companionMessage = crisisResult.isCrisis
    ? 'I am right here with you. You do not have to carry this load alone. You are valuable far beyond your target rank.'
    : burnoutLevel === 'CRISIS_ALERT'
    ? 'The pressure you\'ve been putting on yourself is immense. Please be gentle with yourself today.'
    : checkIn.mood <= 3
    ? 'I am sorry today felt so hard. It is okay to have low days. Take it one hour at a time.'
    : 'You showed up for your wellness check-in today, which is a wonderful step of self-care.';

  return {
    checkInId: checkIn.id || 0,
    timestamp: Date.now(),
    primaryEmotion: checkIn.mood <= 3 ? 'Overwhelmed' : checkIn.anxiety >= 6 ? 'Anxious' : checkIn.motivation >= 7 ? 'Determined' : 'Reflective',
    detectedTriggers,
    intrapersonalScore: intra,
    interpersonalScore: inter,
    academicScore: academic,
    totalBurnoutScore: totalBurnout,
    burnoutLevel,
    suggestion,
    companionMessage
  };
}

// ==================== Gemini API Calls ====================

async function callGemini(systemPrompt: string, userPrompt: string, temperature = 0.3): Promise<string> {
  const apiKey = getApiKey();
  if (!apiKey) throw new Error('API key not configured');

  const response = await fetch(`${GEMINI_API_URL}?key=${apiKey}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      contents: [{ parts: [{ text: userPrompt }] }],
      generationConfig: { temperature, responseMimeType: 'text/plain' },
      systemInstruction: { parts: [{ text: systemPrompt }] }
    })
  });

  if (!response.ok) {
    const err = await response.text();
    throw new Error(`Gemini API error: ${response.status} - ${err}`);
  }

  const data = await response.json();
  return data?.candidates?.[0]?.content?.parts?.[0]?.text || '';
}

async function callGeminiJSON(systemPrompt: string, userPrompt: string): Promise<string> {
  const apiKey = getApiKey();
  if (!apiKey) throw new Error('API key not configured');

  const response = await fetch(`${GEMINI_API_URL}?key=${apiKey}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      contents: [{ parts: [{ text: userPrompt }] }],
      generationConfig: { temperature: 0.2, responseMimeType: 'application/json' },
      systemInstruction: { parts: [{ text: systemPrompt }] }
    })
  });

  if (!response.ok) throw new Error(`Gemini API error: ${response.status}`);
  const data = await response.json();
  return data?.candidates?.[0]?.content?.parts?.[0]?.text || '';
}

// ==================== Journal Analysis ====================

export async function analyzeJournal(checkIn: CheckIn, examType: string): Promise<AIAnalysis> {
  try {
    const systemPrompt = `You are an expert student mental wellness assistant specialized in analyzing daily journal reflections for Indian competitive exam aspirants (JEE, NEET, UPSC, GATE, CAT, CUET, Board exams).
Analyze the provided student's journal and core wellness metrics.
Return ONLY a JSON object with EXACTLY these fields:
{
  "primaryEmotion": "Anxious" / "Overwhelmed" / "Determined" / "Exhausted" / "Lonely" / "Reflective",
  "detectedTriggers": ["up to 3 triggers"],
  "intrapersonalScore": 0-33,
  "interpersonalScore": 0-33,
  "academicScore": 0-33,
  "suggestion": "A practical CBT-aligned wellness suggestion under 120 words.",
  "companionMessage": "A warm, empathetic supportive message under 80 words."
}
Do NOT include markdown blocks or explanation. Return raw JSON only.`;

    const metricsSummary = `Mood: ${checkIn.mood}/10, Stress: ${checkIn.stress}/10, Anxiety: ${checkIn.anxiety}/10, Energy: ${checkIn.energy}/10, Motivation: ${checkIn.motivation}/10, Confidence: ${checkIn.confidence}/10, Sleep: ${checkIn.sleepHours}h (${checkIn.sleepQuality}), Study: ${checkIn.studyHours}h (Breaks: ${checkIn.studyBreaks})`;
    const userPrompt = `Student Exam: ${examType}\nDaily Metrics Summary: ${metricsSummary}\nJournal Entry text: "${checkIn.journalText}"`;

    const jsonText = await callGeminiJSON(systemPrompt, userPrompt);
    const parsed = JSON.parse(jsonText);

    const total = Math.min(100, (parsed.intrapersonalScore || 0) + (parsed.interpersonalScore || 0) + (parsed.academicScore || 0));

    return {
      checkInId: checkIn.id || 0,
      timestamp: Date.now(),
      primaryEmotion: parsed.primaryEmotion || 'Reflective',
      detectedTriggers: parsed.detectedTriggers || [],
      intrapersonalScore: parsed.intrapersonalScore || 0,
      interpersonalScore: parsed.interpersonalScore || 0,
      academicScore: parsed.academicScore || 0,
      totalBurnoutScore: total,
      burnoutLevel: getBurnoutLevel(total),
      suggestion: parsed.suggestion || '',
      companionMessage: parsed.companionMessage || ''
    };
  } catch {
    return generateOfflineAnalysis(checkIn);
  }
}

// ==================== Companion Chat ====================

export async function generateCompanionResponse(
  userInput: string,
  examType: string,
  recentSummary: string,
  messageHistory: ChatMessage[]
): Promise<string> {
  const crisisResult = detectCrisis(userInput);

  try {
    const systemPrompt = `You are MindMate AI, a warm, non-clinical, 24/7 mental wellness companion for Indian competitive exam aspirants (JEE, NEET, UPSC, GATE, CAT, CUET, Boards).
Personality: Warm, empathetic, friend-like. NOT clinical, NOT robotic. Use student's exam context. Validate feelings before offering tools. Never give toxic positive hustle advice.
Safety: Never diagnose. If student mentions suicide/self-harm, gently remind they're not alone and direct to helplines.
Keep responses under 150 words.`;

    const history = messageHistory.slice(-6).map(m =>
      `${m.sender === 'user' ? 'Student' : 'MindMate AI'}: ${m.message}`
    ).join('\n');

    const userPrompt = `[Student Context]\nExam Target: ${examType}\nRecent Check-In Summary: ${recentSummary}\n\n${history}\n\nStudent: ${userInput}\nMindMate AI:`;

    return await callGemini(systemPrompt, userPrompt, 0.7);
  } catch {
    if (crisisResult.isCrisis) return "I'm right here. I hear how overwhelmed you are. Please check our 'Emergency Help' page to connect with counselors who want to support you.";
    if (userInput.toLowerCase().includes('parent')) return "Parental expectations can feel incredibly heavy. Remember, their dreams come from love, but your peace of mind matters most.";
    if (userInput.toLowerCase().includes('backlog')) return "Dealing with backlogs is draining. Try breaking it down—pick one small topic for 20 minutes. You've got this.";
    if (userInput.toLowerCase().includes('mock') || userInput.toLowerCase().includes('score')) return "A mock test measures speed and recall; it does NOT measure your intelligence or potential.";
    return "I hear you, and I'm right here. Let's focus on small, bite-sized steps. What's one small thing we can do right now to help you feel more relaxed?";
  }
}

// ==================== AI Tutor ====================

export async function solveTutorProblem(
  question: string,
  imageBase64: string | null,
  examType: string
): Promise<string> {
  const apiKey = getApiKey();
  if (!apiKey) throw new Error('API key not configured');

  const systemPrompt = `You are MindMate AI Tutor, an exceptionally clear, empathetic, and encouraging expert educator specializing in Indian competitive exams (${examType}).
Solve the problem step-by-step with absolute accuracy and friendly guidance.
- Explain the core concept first
- Provide structured step-by-step breakdown
- End with brief encouragement
- Keep under 220 words, mobile-friendly format`;

  const parts: Array<{ text?: string; inlineData?: { mimeType: string; data: string } }> = [];
  if (imageBase64) {
    parts.push({ inlineData: { mimeType: 'image/jpeg', data: imageBase64 } });
  }
  parts.push({ text: `Student Question: ${question}` });

  const response = await fetch(`${GEMINI_API_URL}?key=${apiKey}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      contents: [{ parts }],
      generationConfig: { temperature: 0.3 },
      systemInstruction: { parts: [{ text: systemPrompt }] }
    })
  });

  if (!response.ok) throw new Error(`Gemini API error: ${response.status}`);
  const data = await response.json();
  return data?.candidates?.[0]?.content?.parts?.[0]?.text || 'Unable to generate solution.';
}

// ==================== Notification Service ====================

export async function requestNotificationPermission(): Promise<boolean> {
  if (!('Notification' in window)) return false;
  if (Notification.permission === 'granted') return true;
  const result = await Notification.requestPermission();
  return result === 'granted';
}

export function scheduleCheckInReminder() {
  if (!('Notification' in window) || Notification.permission !== 'granted') return;

  // Check if we already have a scheduled reminder today
  const lastReminder = localStorage.getItem('mindmate_last_reminder');
  const today = new Date().toDateString();
  if (lastReminder === today) return;

  // Schedule for 8 PM if not already past
  const now = new Date();
  const reminderTime = new Date();
  reminderTime.setHours(20, 0, 0, 0);

  if (now < reminderTime) {
    const delay = reminderTime.getTime() - now.getTime();
    setTimeout(() => {
      new Notification('MindMate AI 🧠', {
        body: "Time for your daily wellness check-in! Take 3 minutes to log how you're feeling today.",
        icon: '/favicon.ico',
        tag: 'daily-checkin',
        requireInteraction: true,
      });
      localStorage.setItem('mindmate_last_reminder', today);
    }, delay);
  }
}

// ==================== Web Speech API ====================

export function startSpeechRecognition(
  onResult: (text: string) => void,
  onPartial: (text: string) => void,
  onEnd: () => void,
  onError: (err: string) => void
): any {
  const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
  if (!SpeechRecognition) {
    onError('Speech recognition not supported in this browser');
    return null;
  }

  const recognition = new SpeechRecognition();
  recognition.lang = 'en-IN';
  recognition.interimResults = true;
  recognition.continuous = false;

  recognition.onresult = (event: any) => {
    let interim = '';
    let final = '';
    for (let i = event.resultIndex; i < event.results.length; i++) {
      if (event.results[i].isFinal) {
        final += event.results[i][0].transcript;
      } else {
        interim += event.results[i][0].transcript;
      }
    }
    if (final) onResult(final);
    if (interim) onPartial(interim);
  };

  recognition.onend = onEnd;
  recognition.onerror = (e: any) => onError(e.error);
  recognition.start();
  return recognition;
}

export function speakText(text: string): SpeechSynthesisUtterance | null {
  if (!('speechSynthesis' in window)) return null;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = 'en-US';
  utterance.rate = 0.95;
  utterance.pitch = 1.0;
  window.speechSynthesis.speak(utterance);
  return utterance;
}
