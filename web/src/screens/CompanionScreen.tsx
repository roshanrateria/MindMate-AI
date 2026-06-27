import { useState, useEffect, useRef, useCallback } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { getChatMessages, insertChatMessage, clearChatHistory, getAllCheckIns } from '@/lib/database';
import { generateCompanionResponse, detectCrisis, startSpeechRecognition, speakText, solveTutorProblem } from '@/lib/gemini';
import type { ChatMessage } from '@/types';
import {
  ArrowLeft, Send, Mic, MicOff, Volume2, Trash2, Loader2,
  Camera, BookOpen, CheckCircle, AlertTriangle, X, Sparkles, Image, GraduationCap
} from 'lucide-react';

const SAMPLE_PROBLEMS = [
  { title: 'JEE Physics - Angular Momentum', question: 'A uniform rod of length L and mass M is pivoted at its center. If a bullet of mass m hits one end of the rod with velocity v, find the angular velocity after impact.', subject: 'Physics' },
  { title: 'NEET Biology - Genetics', question: 'Explain the mechanism of crossing over during meiosis and its significance in genetic variation.', subject: 'Biology' },
  { title: 'JEE Chemistry - Electrochemistry', question: 'Calculate the EMF of a Daniel cell at 25°C when the concentration of ZnSO4 and CuSO4 are 0.001M and 0.1M respectively.', subject: 'Chemistry' },
  { title: 'JEE Maths - Integration', question: 'Evaluate the integral of (x^2 + 1)/(x^4 + 1) dx using partial fractions.', subject: 'Maths' },
];

interface CompanionScreenProps {
  examType: string;
  onBack: () => void;
}

type TabMode = 'chat' | 'tutor';

export function CompanionScreen({ examType, onBack }: CompanionScreenProps) {
  const [mode, setMode] = useState<TabMode>('chat');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isListening, setIsListening] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);
  const recognitionRef = useRef<any>(null);

  // Tutor state
  const [selectedProblem, setSelectedProblem] = useState(0);
  const [customQuestion, setCustomQuestion] = useState('');
  const [tutorState, setTutorState] = useState<'idle' | 'solving' | 'solved' | 'error'>('idle');
  const [tutorSolution, setTutorSolution] = useState('');
  const [tutorError, setTutorError] = useState('');
  const [capturedImage, setCapturedImage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [tutorListening, setTutorListening] = useState(false);

  useEffect(() => {
    loadMessages();
  }, []);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  async function loadMessages() {
    const msgs = await getChatMessages();
    setMessages(msgs);
  }

  async function sendMessage(text?: string) {
    const msg = (text || input).trim();
    if (!msg || isLoading) return;
    setInput('');
    setIsLoading(true);

    const userMsg: Omit<ChatMessage, 'id'> = { timestamp: Date.now(), sender: 'user', message: msg };
    await insertChatMessage(userMsg);
    setMessages(prev => [...prev, { ...userMsg, id: Date.now() }]);

    const crisisResult = detectCrisis(msg);
    const checkIns = await getAllCheckIns();
    const recentSummary = checkIns.slice(0, 7).map(c =>
      `Mood:${c.mood}/10, Stress:${c.stress}/10, Sleep:${c.sleepHours}h`
    ).join(' | ') || 'No recent check-ins';

    const response = await generateCompanionResponse(msg, examType, recentSummary, messages);

    const aiMsg: Omit<ChatMessage, 'id'> = { timestamp: Date.now(), sender: 'ai', message: response };
    await insertChatMessage(aiMsg);
    setMessages(prev => [...prev, { ...aiMsg, id: Date.now() + 1 }]);
    setIsLoading(false);
  }

  function toggleVoice() {
    if (isListening) {
      recognitionRef.current?.stop();
      setIsListening(false);
      return;
    }
    setIsListening(true);
    recognitionRef.current = startSpeechRecognition(
      (text) => { setInput(prev => prev + text); },
      () => {},
      () => { setIsListening(false); },
      () => { setIsListening(false); }
    );
  }

  function toggleTutorVoice() {
    if (tutorListening) {
      recognitionRef.current?.stop();
      setTutorListening(false);
      return;
    }
    setTutorListening(true);
    recognitionRef.current = startSpeechRecognition(
      (text) => { setCustomQuestion(prev => prev + text); },
      () => {},
      () => { setTutorListening(false); },
      () => { setTutorListening(false); }
    );
  }

  async function handleClear() {
    await clearChatHistory();
    setMessages([]);
  }

  function handleImageUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const base64 = (reader.result as string).split(',')[1];
      setCapturedImage(base64);
    };
    reader.readAsDataURL(file);
  }

  async function solveProblem() {
    setTutorState('solving');
    try {
      const problem = SAMPLE_PROBLEMS[selectedProblem];
      const question = customQuestion.trim() || problem.question;
      const finalQ = `Solve this step-by-step with formulas and clear structures: ${question}`;
      const solution = await solveTutorProblem(finalQ, capturedImage, examType);
      setTutorSolution(solution);
      setTutorState('solved');
    } catch (e: any) {
      setTutorError(e.message || 'Failed to solve');
      setTutorState('error');
    }
  }

  return (
    <div className="flex flex-col h-[calc(100dvh-4.5rem)]">
      {/* Header */}
      <div className="flex items-center gap-3 pb-3">
        <button onClick={onBack} className="text-muted-foreground hover:text-white transition-colors">
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div className="flex-1">
          <h1 className="text-lg font-bold text-white">AI Companion</h1>
          <p className="text-xs text-muted-foreground">Empathetic wellness support 24/7</p>
        </div>
        {mode === 'chat' && (
          <Button variant="ghost" size="icon" onClick={handleClear} className="text-muted-foreground">
            <Trash2 className="w-4 h-4" />
          </Button>
        )}
      </div>

      {/* Mode Tabs */}
      <div className="flex gap-2 pb-3">
        {(['chat', 'tutor'] as TabMode[]).map(tab => (
          <button key={tab} onClick={() => setMode(tab)}
            className={`flex-1 flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all ${
              mode === tab ? 'bg-[var(--color-indigo)] text-white shadow-lg shadow-[var(--color-indigo)]/20'
                : 'bg-muted text-muted-foreground hover:text-white'}`}>
            {tab === 'chat' ? <Sparkles className="w-4 h-4" /> : <GraduationCap className="w-4 h-4" />}
            {tab === 'chat' ? 'Empathy Chat' : 'AI Tutor'}
          </button>
        ))}
      </div>

      {/* Chat Mode */}
      {mode === 'chat' ? (
        <>
          <div ref={scrollRef} className="flex-1 overflow-y-auto space-y-3 pr-1 pb-2">
            {messages.length === 0 && (
              <div className="flex flex-col items-center justify-center h-full text-center space-y-3 opacity-60">
                <Sparkles className="w-10 h-10 text-[var(--color-indigo)]" />
                <p className="text-sm text-muted-foreground max-w-[240px]">
                  Hi there! I'm your MindMate companion. Tell me how you're feeling today, or vent about anything that's on your mind.
                </p>
              </div>
            )}

            {messages.map((msg) => (
              <div key={msg.id} className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[85%] px-4 py-2.5 text-sm leading-relaxed ${
                  msg.sender === 'user'
                    ? 'bg-[var(--color-indigo)] text-white rounded-2xl rounded-tr-sm'
                    : 'glass-card rounded-2xl rounded-tl-sm text-white'
                }`}>
                  {msg.message}
                  {msg.sender === 'ai' && (
                    <button onClick={() => speakText(msg.message)}
                      className="mt-1 flex items-center gap-1 text-xs text-muted-foreground hover:text-[var(--color-lavender)] transition-colors">
                      <Volume2 className="w-3 h-3" /> Speak
                    </button>
                  )}
                </div>
              </div>
            ))}

            {isLoading && (
              <div className="flex justify-start">
                <div className="glass-card rounded-2xl rounded-tl-sm px-4 py-3 flex items-center gap-2">
                  <div className="flex gap-1">
                    <div className="w-2 h-2 rounded-full bg-[var(--color-indigo)] animate-bounce" style={{ animationDelay: '0ms' }} />
                    <div className="w-2 h-2 rounded-full bg-[var(--color-lavender)] animate-bounce" style={{ animationDelay: '150ms' }} />
                    <div className="w-2 h-2 rounded-full bg-[var(--color-teal)] animate-bounce" style={{ animationDelay: '300ms' }} />
                  </div>
                  <span className="text-xs text-muted-foreground">Thinking...</span>
                </div>
              </div>
            )}
          </div>

          {/* Input */}
          <div className="pt-3 border-t border-[var(--color-glass-border)]">
            <div className="flex gap-2 items-end">
              <div className="flex-1 glass-card rounded-2xl flex items-center px-3">
                <input
                  value={input}
                  onChange={e => setInput(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && sendMessage()}
                  placeholder="Share how you're feeling..."
                  className="flex-1 bg-transparent border-none py-3 text-sm text-white placeholder:text-muted-foreground focus:outline-none"
                />
                <button onClick={toggleVoice}
                  className={`p-2 rounded-full transition-all ${isListening ? 'bg-[var(--color-red-crisis)] text-white animate-pulse' : 'text-muted-foreground hover:text-white'}`}>
                  {isListening ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
                </button>
              </div>
              <Button onClick={() => sendMessage()} disabled={!input.trim() || isLoading}
                size="icon" className="h-11 w-11 rounded-full shrink-0">
                <Send className="w-4 h-4" />
              </Button>
            </div>
          </div>
        </>
      ) : (
        /* Tutor Mode */
        <div className="flex-1 overflow-y-auto space-y-4 pb-4">
          {/* Camera / Upload */}
          <Card className="glass-card overflow-hidden">
            <CardContent className="p-4 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Camera className="w-5 h-5 text-[var(--color-indigo)]" />
                  <span className="text-white font-semibold text-sm">Upload Problem Photo</span>
                </div>
                {capturedImage && (
                  <button onClick={() => setCapturedImage(null)} className="text-muted-foreground hover:text-[var(--color-red-crisis)]">
                    <X className="w-4 h-4" />
                  </button>
                )}
              </div>

              {capturedImage ? (
                <div className="relative rounded-xl overflow-hidden border border-[var(--color-glass-border)]">
                  <img src={`data:image/jpeg;base64,${capturedImage}`} alt="Problem"
                    className="w-full max-h-52 object-contain bg-black/20" />
                  <div className="absolute bottom-2 left-2 px-2 py-0.5 rounded-md bg-[var(--color-green-resilient)]/20 text-[var(--color-green-resilient)] text-xs font-medium">
                    ✓ Image loaded
                  </div>
                </div>
              ) : (
                <button onClick={() => fileInputRef.current?.click()}
                  className="w-full border-2 border-dashed border-[var(--color-glass-border)] rounded-xl py-8 flex flex-col items-center gap-2 hover:border-[var(--color-indigo)]/50 transition-colors">
                  <Image className="w-8 h-8 text-muted-foreground" />
                  <span className="text-sm text-muted-foreground">Tap to upload textbook photo</span>
                </button>
              )}
              <input ref={fileInputRef} type="file" accept="image/*" capture="environment"
                className="hidden" onChange={handleImageUpload} />
            </CardContent>
          </Card>

          {/* Custom Question */}
          <div className="space-y-1.5">
            <p className="text-xs text-muted-foreground">Ask Tutor a Specific Question (or use preset below):</p>
            <div className="flex gap-2">
              <input
                value={customQuestion}
                onChange={e => setCustomQuestion(e.target.value)}
                placeholder="What do you want me to explain here?"
                className="flex-1 bg-[var(--color-surface-card)] border border-[var(--color-glass-border)] rounded-xl px-4 py-3 text-sm text-white placeholder:text-muted-foreground focus:outline-none focus:border-[var(--color-indigo)]"
              />
              <button onClick={toggleTutorVoice}
                className={`w-12 h-12 rounded-full flex items-center justify-center transition-all ${
                  tutorListening ? 'bg-[var(--color-red-crisis)] text-white animate-pulse' : 'bg-[var(--color-glass-border)] text-white hover:bg-muted'}`}>
                {tutorListening ? <X className="w-5 h-5" /> : <Mic className="w-5 h-5" />}
              </button>
            </div>
          </div>

          {/* Problem Presets */}
          <div className="space-y-2">
            <p className="text-xs text-muted-foreground">Or Choose Study Problem Preset:</p>
            <div className="flex gap-2 overflow-x-auto pb-2 -mx-1 px-1">
              {SAMPLE_PROBLEMS.map((p, i) => (
                <button key={i} onClick={() => setSelectedProblem(i)}
                  className={`shrink-0 w-40 rounded-xl p-3 text-left border transition-all ${
                    selectedProblem === i
                      ? 'bg-[var(--color-indigo)]/30 border-[var(--color-indigo)]'
                      : 'bg-[var(--color-surface-card)] border-transparent hover:border-[var(--color-glass-border)]'}`}>
                  <p className={`text-xs font-bold ${selectedProblem === i ? 'text-white' : 'text-muted-foreground'}`}>
                    {p.subject}
                  </p>
                  <p className="text-xs text-white mt-1 line-clamp-1">{p.title.split(' - ').pop()}</p>
                </button>
              ))}
            </div>
          </div>

          {/* Solve Button */}
          <Button onClick={solveProblem} disabled={tutorState === 'solving'} className="w-full gap-2">
            {tutorState === 'solving' ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle className="w-4 h-4" />}
            {tutorState === 'solving' ? 'Analyzing & Solving...' : 'Snap & Solve Problem'}
          </Button>

          {/* Solution Display */}
          {tutorState === 'solving' && (
            <Card className="glass-card">
              <CardContent className="p-5 flex flex-col items-center gap-3">
                <Loader2 className="w-8 h-8 animate-spin text-[var(--color-indigo)]" />
                <p className="text-sm text-white text-center">Gemini is analyzing diagram & solving problem...</p>
              </CardContent>
            </Card>
          )}

          {tutorState === 'solved' && (
            <Card className="glass-card">
              <CardContent className="p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-bold text-[var(--color-green-resilient)]">✍️ Step-by-Step Solution</p>
                  <button onClick={() => speakText(tutorSolution)}
                    className="p-2 rounded-full hover:bg-muted transition-colors">
                    <Volume2 className="w-4 h-4 text-[var(--color-lavender)]" />
                  </button>
                </div>
                <p className="text-sm text-white leading-relaxed whitespace-pre-wrap">{tutorSolution}</p>
              </CardContent>
            </Card>
          )}

          {tutorState === 'error' && (
            <Card className="glass-card">
              <CardContent className="p-4 flex items-center gap-3">
                <AlertTriangle className="w-5 h-5 text-[var(--color-red-crisis)] shrink-0" />
                <p className="text-sm text-white">{tutorError}</p>
              </CardContent>
            </Card>
          )}

          {tutorState === 'idle' && (
            <Card className="glass-card">
              <CardContent className="p-5 flex flex-col items-center gap-2 text-center">
                <BookOpen className="w-9 h-9 text-muted-foreground" />
                <p className="text-sm text-muted-foreground">Choose a study problem card above and click Snap & Solve to launch active tutor session.</p>
              </CardContent>
            </Card>
          )}
        </div>
      )}
    </div>
  );
}
