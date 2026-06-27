import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { getAllCheckIns, getAllAnalyses } from '@/lib/database';
import { getWellnessScore, calculateStreak, getBurnoutLevel } from '@/lib/gemini';
import type { DashboardState } from '@/types';
import { Plus, MessageCircle, AlertTriangle, Flame, Wrench, TrendingUp, Heart, Sparkles, Star, Info } from 'lucide-react';

interface DashboardScreenProps {
  nickname: string;
  onNavigate: (route: string) => void;
}

const BURNOUT_COLORS: Record<string, string> = {
  RESILIENT: '#42E6A4',
  CAUTION: '#FFD166',
  AT_RISK: '#F28F3B',
  CRISIS_ALERT: '#EF5350',
};

const BURNOUT_LABELS: Record<string, string> = {
  RESILIENT: 'Resilient (Green)',
  CAUTION: 'Caution (Yellow)',
  AT_RISK: 'At Risk (Orange)',
  CRISIS_ALERT: 'Crisis Alert (Red)',
};

export function DashboardScreen({ nickname, onNavigate }: DashboardScreenProps) {
  const [state, setState] = useState<DashboardState>({
    recentCheckIns: [],
    overallWellnessScore: 0,
    burnoutScore: 0,
    burnoutLevel: 'RESILIENT',
    latestAnalysis: null,
    streakCount: 0,
    intrapersonalBurnout: 0,
    interpersonalBurnout: 0,
    academicBurnout: 0,
    triggerCounts: {},
    hasCrisisScore: false,
  });

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    const checkIns = await getAllCheckIns();
    const analyses = await getAllAnalyses();
    if (checkIns.length === 0) {
      setState(prev => ({ ...prev, recentCheckIns: [] }));
      return;
    }

    const recent = checkIns.slice(0, 30);
    const latestCheckIn = recent[0];
    const wellnessScore = latestCheckIn ? getWellnessScore(latestCheckIn) : 0;
    const latestAnalysis = analyses[0] || null;
    const burnoutScore = latestAnalysis?.totalBurnoutScore || 0;
    const burnoutLevel = latestAnalysis?.burnoutLevel || getBurnoutLevel(0);
    const streak = calculateStreak(checkIns);

    const triggerMap: Record<string, number> = {};
    checkIns.forEach(ci => ci.triggers.forEach(t => { triggerMap[t] = (triggerMap[t] || 0) + 1; }));

    setState({
      recentCheckIns: recent,
      overallWellnessScore: wellnessScore,
      burnoutScore,
      burnoutLevel,
      latestAnalysis,
      streakCount: streak,
      intrapersonalBurnout: latestAnalysis?.intrapersonalScore || 0,
      interpersonalBurnout: latestAnalysis?.interpersonalScore || 0,
      academicBurnout: latestAnalysis?.academicScore || 0,
      triggerCounts: triggerMap,
      hasCrisisScore: burnoutScore >= 76,
    });
  }

  const scoreColor = state.overallWellnessScore >= 70 ? '#42E6A4' : state.overallWellnessScore >= 40 ? '#FFD166' : '#EF5350';
  const circumference = 2 * Math.PI * 56;
  const strokeDashoffset = circumference - (state.overallWellnessScore / 100) * circumference;

  return (
    <div className="space-y-5 pb-6">
      {/* Greeting */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white">Hello {nickname || 'MindMate'} 👋</h1>
          <p className="text-sm text-muted-foreground">Take it one small concept at a time.</p>
        </div>
        <Button variant="outline" size="sm" onClick={() => onNavigate('crisis')} className="border-[var(--color-red-crisis)]/50 text-[var(--color-red-crisis)] gap-1.5">
          <AlertTriangle className="w-4 h-4" />
          <span className="hidden sm:inline">Urgent Help</span>
        </Button>
      </div>

      {/* Streak Card */}
      <div className="glass-card rounded-2xl p-4 flex items-center gap-4">
        <div className="w-14 h-14 rounded-full bg-[var(--color-indigo)]/20 flex items-center justify-center">
          <Star className="w-7 h-7 text-[var(--color-orange-risk)]" />
        </div>
        <div>
          <p className="text-xl font-bold text-white">{state.streakCount} Day Streak!</p>
          <p className="text-sm text-muted-foreground">
            {state.streakCount >= 7 ? 'Awesome! You are cultivating deep mental resilience.' :
             state.streakCount >= 3 ? 'Consistency is key. Keep prioritizing your wellness!' :
             'Every single check-in counts. You are doing great!'}
          </p>
        </div>
      </div>

      {/* Wellness Score Ring */}
      <Card className="glass-card overflow-hidden">
        <CardContent className="p-6 flex flex-col items-center space-y-5">
          <div className="text-center">
            <h2 className="text-white font-bold">My General Wellness Rating</h2>
            <p className="text-xs text-muted-foreground">Calculated from sleep, breaks, confidence, and mood</p>
          </div>

          <div className="relative w-36 h-36">
            <svg className="w-full h-full -rotate-90" viewBox="0 0 128 128">
              <circle cx="64" cy="64" r="56" fill="none" stroke="currentColor" strokeWidth="10" className="text-muted/30" />
              <circle cx="64" cy="64" r="56" fill="none" stroke={scoreColor} strokeWidth="10"
                strokeDasharray={circumference} strokeDashoffset={strokeDashoffset}
                strokeLinecap="round" className="transition-all duration-1000" />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className="text-4xl font-black text-white">{state.overallWellnessScore}</span>
              <span className="text-xs text-muted-foreground">Score</span>
            </div>
          </div>

          <div className="flex gap-3 w-full">
            <Button onClick={() => onNavigate('checkin')} className="flex-1 gap-1.5">
              <Plus className="w-4 h-4" /> New Log
            </Button>
            <Button onClick={() => onNavigate('companion')} variant="secondary" className="flex-1 gap-1.5">
              <MessageCircle className="w-4 h-4" /> AI Companion
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Burnout Meter */}
      {state.recentCheckIns.length > 0 ? (
        <Card className="glass-card">
          <CardContent className="p-5 space-y-4">
            <h3 className="text-white font-bold flex items-center gap-2">
              <Flame className="w-5 h-5 text-[var(--color-orange-risk)]" /> Burnout Risk Meter
            </h3>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-muted-foreground">Overall Burnout Risk</p>
                <p className="font-bold text-lg" style={{ color: BURNOUT_COLORS[state.burnoutLevel] }}>
                  {BURNOUT_LABELS[state.burnoutLevel]}
                </p>
              </div>
              <span className="text-3xl font-black" style={{ color: BURNOUT_COLORS[state.burnoutLevel] }}>
                {state.burnoutScore}/100
              </span>
            </div>
            <Progress value={state.burnoutScore} indicatorColor={BURNOUT_COLORS[state.burnoutLevel]} />

            <div className="pt-2 border-t border-[var(--color-glass-border)] space-y-3">
              <p className="text-xs font-semibold text-muted-foreground">Three-Domain Academic Burnout Breakdown:</p>
              {[
                { label: 'Intrapersonal (Fatigue, Sleep)', value: state.intrapersonalBurnout, color: '#7F7FD5' },
                { label: 'Interpersonal (Social Withdrawal)', value: state.interpersonalBurnout, color: '#86A8E7' },
                { label: 'Academic (Motivation Loss)', value: state.academicBurnout, color: '#91EAE4' },
              ].map(d => (
                <div key={d.label} className="space-y-1">
                  <div className="flex justify-between text-xs">
                    <span className="text-muted-foreground">{d.label}</span>
                    <span className="font-bold" style={{ color: d.color }}>{d.value}/33</span>
                  </div>
                  <Progress value={(d.value / 33) * 100} indicatorColor={d.color} className="h-1.5" />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card className="glass-card">
          <CardContent className="p-6 text-center space-y-3">
            <Info className="w-10 h-10 text-muted-foreground mx-auto" />
            <h3 className="text-white font-bold">No wellness logs found</h3>
            <p className="text-sm text-muted-foreground">Log your first daily check-in to activate burnout analysis and AI trends.</p>
            <Button onClick={() => onNavigate('checkin')}>Log Daily Wellness Now</Button>
          </CardContent>
        </Card>
      )}

      {/* Stress Triggers */}
      {Object.keys(state.triggerCounts).length > 0 && (
        <Card className="glass-card">
          <CardContent className="p-5 space-y-3">
            <h3 className="text-white font-bold flex items-center gap-2">
              <TrendingUp className="w-5 h-5 text-[var(--color-indigo)]" /> My Core Stress Triggers
            </h3>
            <p className="text-xs text-muted-foreground">Stressors showing up in your recent logs</p>
            <div className="space-y-2">
              {Object.entries(state.triggerCounts)
                .sort(([, a], [, b]) => b - a)
                .map(([trigger, count]) => (
                  <div key={trigger} className="flex items-center gap-2">
                    <div className="flex-1 bg-[var(--color-indigo)]/10 rounded-lg px-3 py-1.5 text-sm text-white truncate">
                      {trigger}
                    </div>
                    <div className="w-8 h-8 rounded-full bg-[var(--color-indigo)]/20 flex items-center justify-center">
                      <span className="text-xs font-bold text-[var(--color-indigo)]">{count}</span>
                    </div>
                  </div>
                ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Toolkit Shortcut */}
      <button onClick={() => onNavigate('toolkit')} className="w-full glass-card rounded-2xl p-4 flex items-center gap-3 text-left hover:border-[var(--color-teal)]/30 transition-colors">
        <div className="w-10 h-10 rounded-full bg-[var(--color-teal)]/20 flex items-center justify-center">
          <Wrench className="w-5 h-5 text-[var(--color-teal)]" />
        </div>
        <div>
          <p className="text-white font-bold">Explore Wellness Toolkit</p>
          <p className="text-xs text-muted-foreground">Box Breathing, muscle relaxation, panic grounding</p>
        </div>
      </button>
    </div>
  );
}
