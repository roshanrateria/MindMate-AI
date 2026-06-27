import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { GraduationCap, AlertTriangle, ArrowRight, Brain } from 'lucide-react';
import type { OnboardingData } from '@/types';

const EXAMS = ['JEE (Engineering)', 'NEET (Medical)', 'UPSC (Civil Services)', 'GATE', 'CAT', 'CUET', 'CBSE/ICSE Boards', 'Other'];
const WORRIES = ['Parental Expectations', 'Fear of Failure', 'Peer Comparison', 'Volume of Mock Tests', 'Syllabus Backlog', 'Homesickness & Loneliness'];

interface OnboardingScreenProps {
  onComplete: (data: OnboardingData) => void;
}

export function OnboardingScreen({ onComplete }: OnboardingScreenProps) {
  const [nickname, setNickname] = useState('');
  const [examType, setExamType] = useState('JEE (Engineering)');
  const [biggestWorry, setBiggestWorry] = useState('Parental Expectations');

  const handleSubmit = () => {
    if (nickname.trim()) {
      onComplete({ nickname: nickname.trim(), examType, biggestWorry, completed: true });
    }
  };

  return (
    <div className="min-h-screen bg-background relative overflow-hidden">
      {/* Ambient gradient */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--color-indigo)_0%,_transparent_60%)] opacity-10 pointer-events-none" />

      <div className="relative z-10 max-w-lg mx-auto px-5 py-8 space-y-6">
        {/* Brand */}
        <div className="text-center space-y-2 pt-8">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="w-14 h-14 rounded-2xl gradient-indigo flex items-center justify-center shadow-lg shadow-[var(--color-indigo)]/30">
              <Brain className="w-8 h-8 text-white" />
            </div>
          </div>
          <h1 className="text-4xl font-black gradient-text">MindMate AI</h1>
          <p className="text-muted-foreground text-base">Your safe, stigma-free exam wellness space</p>
        </div>

        {/* Step 1: Nickname */}
        <Card className="glass-card">
          <CardContent className="p-5 space-y-3">
            <h2 className="text-white font-bold">1. What should we call you?</h2>
            <p className="text-xs text-muted-foreground">Your data stays entirely in your browser.</p>
            <input
              value={nickname}
              onChange={e => setNickname(e.target.value)}
              placeholder="Enter nickname / pseudonym"
              className="w-full bg-background border border-[var(--color-glass-border)] rounded-xl px-4 py-3 text-white placeholder:text-muted-foreground focus:outline-none focus:border-[var(--color-indigo)] transition-colors"
            />
          </CardContent>
        </Card>

        {/* Step 2: Exam Type */}
        <Card className="glass-card">
          <CardContent className="p-5 space-y-3">
            <div className="flex items-center gap-2">
              <GraduationCap className="w-5 h-5 text-[var(--color-lavender)]" />
              <h2 className="text-white font-bold">2. Select your target exam</h2>
            </div>
            <div className="grid grid-cols-2 gap-2">
              {EXAMS.map(exam => (
                <button
                  key={exam}
                  onClick={() => setExamType(exam)}
                  className={`px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 border ${
                    examType === exam
                      ? 'bg-[var(--color-indigo)]/20 border-[var(--color-indigo)] text-[var(--color-indigo)]'
                      : 'bg-background border-[var(--color-glass-border)] text-muted-foreground hover:border-muted-foreground/50'
                  }`}
                >
                  {exam}
                </button>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Step 3: Biggest Worry */}
        <Card className="glass-card">
          <CardContent className="p-5 space-y-3">
            <div className="flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-[var(--color-orange-risk)]" />
              <h2 className="text-white font-bold">3. What's your biggest stressor?</h2>
            </div>
            <div className="space-y-2">
              {WORRIES.map(worry => (
                <button
                  key={worry}
                  onClick={() => setBiggestWorry(worry)}
                  className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-200 border text-left ${
                    biggestWorry === worry
                      ? 'bg-[var(--color-orange-risk)]/15 border-[var(--color-orange-risk)] text-[var(--color-orange-risk)]'
                      : 'bg-background border-[var(--color-glass-border)] text-muted-foreground hover:border-muted-foreground/50'
                  }`}
                >
                  <div className={`w-4 h-4 rounded-full border-2 flex items-center justify-center ${
                    biggestWorry === worry ? 'border-[var(--color-orange-risk)]' : 'border-muted-foreground/50'
                  }`}>
                    {biggestWorry === worry && <div className="w-2 h-2 rounded-full bg-[var(--color-orange-risk)]" />}
                  </div>
                  {worry}
                </button>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Submit */}
        <Button
          onClick={handleSubmit}
          disabled={!nickname.trim()}
          size="lg"
          className="w-full h-14 text-base font-bold"
        >
          Enter MindMate Space
          <ArrowRight className="w-5 h-5" />
        </Button>

        <div className="h-8" />
      </div>
    </div>
  );
}
