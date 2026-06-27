import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Slider } from '@/components/ui/slider';
import { Switch } from '@/components/ui/switch';
import { Progress } from '@/components/ui/progress';
import { insertCheckIn, insertAnalysis } from '@/lib/database';
import { analyzeJournal } from '@/lib/gemini';
import type { CheckIn, AIAnalysis, BurnoutLevel } from '@/types';
import {
  Smile, Brain, Zap, Battery, Target, Shield,
  Moon, BookOpen, Utensils, ArrowLeft, Loader2, Send,
  Sparkles, Info, TrendingUp, Heart
} from 'lucide-react';

const TRIGGERS = [
  '📊 Mock Test Scores', '👨‍👩‍👧 Parental Expectations', '🏆 Peer Comparison',
  '📚 Syllabus Backlog', '😔 Feeling Alone', '🏠 Homesickness',
  '💰 Financial Pressure', '😴 Sleep Deprivation'
];

const SLEEP_OPTIONS: Array<CheckIn['sleepQuality']> = ['Restful', 'Disturbed', "Couldn't sleep"];
const MEAL_OPTIONS: Array<CheckIn['mealsEaten']> = ['Yes', 'Skipped some', 'Barely ate'];

const BURNOUT_COLORS: Record<BurnoutLevel, string> = {
  RESILIENT: '#42E6A4', CAUTION: '#FFD166', AT_RISK: '#F28F3B', CRISIS_ALERT: '#EF5350',
};

interface CheckInScreenProps {
  examType: string;
  onComplete: () => void;
  onBack: () => void;
}

export function CheckInScreen({ examType, onComplete, onBack }: CheckInScreenProps) {
  const [mood, setMood] = useState(5);
  const [stress, setStress] = useState(5);
  const [anxiety, setAnxiety] = useState(5);
  const [energy, setEnergy] = useState(5);
  const [motivation, setMotivation] = useState(5);
  const [confidence, setConfidence] = useState(5);
  const [sleepHours, setSleepHours] = useState(7);
  const [sleepQuality, setSleepQuality] = useState<CheckIn['sleepQuality']>('Restful');
  const [studyHours, setStudyHours] = useState(6);
  const [studyBreaks, setStudyBreaks] = useState(true);
  const [mealsEaten, setMealsEaten] = useState<CheckIn['mealsEaten']>('Yes');
  const [selectedTriggers, setSelectedTriggers] = useState<string[]>([]);
  const [journal, setJournal] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [analysis, setAnalysis] = useState<AIAnalysis | null>(null);

  const toggleTrigger = (t: string) => {
    setSelectedTriggers(prev => prev.includes(t) ? prev.filter(x => x !== t) : [...prev, t]);
  };

  async function handleSubmit() {
    setIsSubmitting(true);
    try {
      const checkIn: Omit<CheckIn, 'id'> = {
        timestamp: Date.now(),
        mood, stress, anxiety, energy, motivation, confidence,
        sleepHours, sleepQuality, studyHours, studyBreaks, mealsEaten,
        triggers: selectedTriggers, journalText: journal,
      };
      const checkInId = await insertCheckIn(checkIn);
      const result = await analyzeJournal({ ...checkIn, id: checkInId }, examType);
      const savedAnalysis = { ...result, checkInId };
      await insertAnalysis(savedAnalysis);
      setAnalysis(savedAnalysis);
    } catch (e) {
      console.error(e);
    } finally {
      setIsSubmitting(false);
    }
  }

  if (analysis) {
    const bColor = BURNOUT_COLORS[analysis.burnoutLevel];
    return (
      <div className="space-y-5 pb-6">
        <button onClick={onComplete} className="flex items-center gap-2 text-muted-foreground hover:text-white transition-colors">
          <ArrowLeft className="w-4 h-4" /> Back to Dashboard
        </button>

        {/* AI Analysis Card */}
        <div className="glass-card rounded-2xl p-5 space-y-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-[var(--color-indigo)]/20 flex items-center justify-center">
              <Sparkles className="w-5 h-5 text-[var(--color-indigo)]" />
            </div>
            <div>
              <h3 className="text-white font-bold">AI Wellness Insights</h3>
              <p className="text-xs text-muted-foreground">Analyzed dynamically via Gemini Flash</p>
            </div>
          </div>

          <div className="flex gap-2 flex-wrap">
            <span className="px-3 py-1 rounded-full bg-[var(--color-indigo)]/15 text-[var(--color-indigo)] text-sm font-medium">
              Emotion: {analysis.primaryEmotion}
            </span>
            <span className="px-3 py-1 rounded-full text-sm font-medium" style={{ backgroundColor: `${bColor}15`, color: bColor }}>
              Burnout: {analysis.totalBurnoutScore}/100
            </span>
          </div>

          {analysis.detectedTriggers.length > 0 && (
            <div className="space-y-2">
              <p className="text-xs font-semibold text-muted-foreground">Stress Triggers Detected:</p>
              <div className="flex flex-wrap gap-1.5">
                {analysis.detectedTriggers.map(t => (
                  <span key={t} className="px-2.5 py-1 rounded-lg bg-muted text-muted-foreground text-xs">{t}</span>
                ))}
              </div>
            </div>
          )}

          <div className="border-t border-[var(--color-glass-border)] pt-4 space-y-1">
            <p className="text-xs font-semibold text-[var(--color-indigo)]">Message from Companion:</p>
            <p className="text-sm text-white leading-relaxed italic">"{analysis.companionMessage}"</p>
          </div>

          <Card className="bg-muted/50 border-none">
            <CardContent className="p-3 space-y-1.5">
              <div className="flex items-center gap-1.5">
                <Info className="w-4 h-4 text-[var(--color-yellow-caution)]" />
                <p className="text-xs font-bold text-[var(--color-yellow-caution)]">Actionable Suggestion</p>
              </div>
              <p className="text-sm text-muted-foreground leading-relaxed">{analysis.suggestion}</p>
            </CardContent>
          </Card>
        </div>

        <Button onClick={onComplete} className="w-full">Return to Dashboard</Button>
      </div>
    );
  }

  return (
    <div className="space-y-4 pb-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <button onClick={onBack} className="text-muted-foreground hover:text-white transition-colors">
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-xl font-bold text-white">Daily Wellness Log</h1>
          <p className="text-xs text-muted-foreground">Honest answers enable smarter AI guidance</p>
        </div>
      </div>

      {/* Sliders */}
      {[
        { label: 'Overall Mood', value: mood, set: setMood, icon: Smile, color: '#7F7FD5', low: 'Terrible', high: 'Amazing' },
        { label: 'Stress Level', value: stress, set: setStress, icon: Brain, color: '#EF5350', low: 'Calm', high: 'Extremely stressed' },
        { label: 'Anxiety Level', value: anxiety, set: setAnxiety, icon: Zap, color: '#F28F3B', low: 'Relaxed', high: 'Very anxious' },
        { label: 'Energy Level', value: energy, set: setEnergy, icon: Battery, color: '#42E6A4', low: 'Drained', high: 'Energized' },
        { label: 'Motivation', value: motivation, set: setMotivation, icon: Target, color: '#FFD166', low: 'Unmotivated', high: 'Driven' },
        { label: 'Confidence', value: confidence, set: setConfidence, icon: Shield, color: '#91EAE4', low: 'Low', high: 'Very confident' },
      ].map(item => (
        <Card key={item.label} className="glass-card">
          <CardContent className="p-4 space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <item.icon className="w-5 h-5" style={{ color: item.color }} />
                <span className="text-white font-semibold text-sm">{item.label}</span>
              </div>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold" style={{ backgroundColor: `${item.color}33`, color: item.color }}>
                {item.value}/10
              </span>
            </div>
            <Slider
              value={[item.value]}
              onValueChange={v => item.set(v[0])}
              min={0} max={10} step={1}
              trackColor={item.color}
            />
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>{item.low}</span><span>{item.high}</span>
            </div>
          </CardContent>
        </Card>
      ))}

      {/* Sleep */}
      <Card className="glass-card">
        <CardContent className="p-4 space-y-3">
          <div className="flex items-center gap-2">
            <Moon className="w-5 h-5 text-[var(--color-lavender)]" />
            <span className="text-white font-semibold text-sm">Sleep</span>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-muted-foreground text-sm">{sleepHours}h</span>
            <Slider value={[sleepHours]} onValueChange={v => setSleepHours(v[0])} min={0} max={14} step={0.5}
              trackColor="#86A8E7" className="flex-1" />
          </div>
          <div className="flex gap-2">
            {SLEEP_OPTIONS.map(opt => (
              <button key={opt} onClick={() => setSleepQuality(opt)}
                className={`flex-1 px-2 py-2 rounded-xl text-xs font-medium border transition-all ${
                  sleepQuality === opt ? 'bg-[var(--color-lavender)]/20 border-[var(--color-lavender)] text-[var(--color-lavender)]'
                    : 'border-[var(--color-glass-border)] text-muted-foreground'}`}>
                {opt}
              </button>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Study & Breaks */}
      <Card className="glass-card">
        <CardContent className="p-4 space-y-3">
          <div className="flex items-center gap-2">
            <BookOpen className="w-5 h-5 text-[var(--color-teal)]" />
            <span className="text-white font-semibold text-sm">Study Hours</span>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-muted-foreground text-sm">{studyHours}h</span>
            <Slider value={[studyHours]} onValueChange={v => setStudyHours(v[0])} min={0} max={16} step={0.5}
              trackColor="#91EAE4" className="flex-1" />
          </div>
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">Taking regular study breaks?</span>
            <Switch checked={studyBreaks} onCheckedChange={setStudyBreaks} />
          </div>
        </CardContent>
      </Card>

      {/* Meals */}
      <Card className="glass-card">
        <CardContent className="p-4 space-y-3">
          <div className="flex items-center gap-2">
            <Utensils className="w-5 h-5 text-[var(--color-green-resilient)]" />
            <span className="text-white font-semibold text-sm">Meals Today</span>
          </div>
          <div className="flex gap-2">
            {MEAL_OPTIONS.map(opt => (
              <button key={opt} onClick={() => setMealsEaten(opt)}
                className={`flex-1 px-2 py-2 rounded-xl text-xs font-medium border transition-all ${
                  mealsEaten === opt ? 'bg-[var(--color-green-resilient)]/20 border-[var(--color-green-resilient)] text-[var(--color-green-resilient)]'
                    : 'border-[var(--color-glass-border)] text-muted-foreground'}`}>
                {opt}
              </button>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Stress Triggers */}
      <Card className="glass-card">
        <CardContent className="p-4 space-y-3">
          <h3 className="text-white font-semibold text-sm">Key Stress Triggers (Select all that apply)</h3>
          <div className="flex flex-wrap gap-2">
            {TRIGGERS.map(t => (
              <button key={t} onClick={() => toggleTrigger(t)}
                className={`px-3 py-1.5 rounded-full text-xs font-medium border transition-all ${
                  selectedTriggers.includes(t) ? 'bg-[var(--color-indigo)]/20 border-[var(--color-indigo)] text-[var(--color-indigo)]'
                    : 'border-[var(--color-glass-border)] text-muted-foreground'}`}>
                {t}
              </button>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Journal */}
      <Card className="glass-card">
        <CardContent className="p-4 space-y-3">
          <h3 className="text-white font-semibold text-sm">📔 Micro-Journal (Optional)</h3>
          <p className="text-xs text-muted-foreground">Freely vent. This stays private in your browser.</p>
          <textarea
            value={journal}
            onChange={e => setJournal(e.target.value)}
            placeholder="How was your day? What's weighing on your mind?"
            rows={4}
            className="w-full bg-background border border-[var(--color-glass-border)] rounded-xl px-4 py-3 text-white text-sm placeholder:text-muted-foreground focus:outline-none focus:border-[var(--color-indigo)] transition-colors resize-none"
          />
        </CardContent>
      </Card>

      {/* Submit */}
      <Button onClick={handleSubmit} disabled={isSubmitting} className="w-full h-12 text-base gap-2" size="lg">
        {isSubmitting ? <Loader2 className="w-5 h-5 animate-spin" /> : <Send className="w-5 h-5" />}
        {isSubmitting ? 'Analyzing with AI...' : 'Submit & Get AI Analysis'}
      </Button>
    </div>
  );
}
