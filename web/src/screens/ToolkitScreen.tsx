import { useState, useEffect, useCallback } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { ArrowLeft, Wind, Dumbbell, Eye, Moon, Play, Pause, RotateCcw } from 'lucide-react';

type ToolType = 'breathing' | 'pmr' | 'grounding' | 'sleep';

const TOOLS: { key: ToolType; label: string; icon: any }[] = [
  { key: 'breathing', label: 'Box Breathing', icon: Wind },
  { key: 'pmr', label: 'PMR Relaxation', icon: Dumbbell },
  { key: 'grounding', label: '5-4-3-2-1 Grounding', icon: Eye },
  { key: 'sleep', label: 'Sleep Checklist', icon: Moon },
];

interface ToolkitScreenProps {
  onBack: () => void;
}

export function ToolkitScreen({ onBack }: ToolkitScreenProps) {
  const [tool, setTool] = useState<ToolType>('breathing');

  return (
    <div className="space-y-4 pb-6">
      <div className="flex items-center gap-3">
        <button onClick={onBack} className="text-muted-foreground hover:text-white transition-colors">
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-xl font-bold text-[var(--color-indigo)]">Wellness Toolkit</h1>
          <p className="text-xs text-muted-foreground">Evidence-based tools to soothe study exhaustion</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 overflow-x-auto pb-1 -mx-1 px-1">
        {TOOLS.map(t => (
          <button key={t.key} onClick={() => setTool(t.key)}
            className={`shrink-0 flex items-center gap-1.5 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all ${
              tool === t.key ? 'bg-[var(--color-indigo)] text-white shadow-lg shadow-[var(--color-indigo)]/20'
                : 'bg-muted text-muted-foreground hover:text-white'}`}>
            <t.icon className="w-4 h-4" />
            {t.label}
          </button>
        ))}
      </div>

      {tool === 'breathing' && <BoxBreathingSection />}
      {tool === 'pmr' && <PMRSection />}
      {tool === 'grounding' && <GroundingSection />}
      {tool === 'sleep' && <SleepChecklistSection />}
    </div>
  );
}

// ==================== Box Breathing ====================

function BoxBreathingSection() {
  const [isRunning, setIsRunning] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(4);
  const [phase, setPhase] = useState<'Inhale' | 'Hold (Full)' | 'Exhale' | 'Hold (Empty)'>('Inhale');

  useEffect(() => {
    if (!isRunning) return;
    const timer = setInterval(() => {
      setSecondsLeft(prev => {
        if (prev > 1) return prev - 1;
        setPhase(p => {
          switch (p) {
            case 'Inhale': return 'Hold (Full)';
            case 'Hold (Full)': return 'Exhale';
            case 'Exhale': return 'Hold (Empty)';
            default: return 'Inhale';
          }
        });
        return 4;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [isRunning]);

  const phaseColors: Record<string, string> = {
    'Inhale': '#42E6A4',
    'Hold (Full)': '#7F7FD5',
    'Exhale': '#91EAE4',
    'Hold (Empty)': '#F28F3B',
  };

  const bubbleSize = (phase === 'Inhale' || phase === 'Hold (Full)') ? 180 : 110;
  const color = phaseColors[phase];

  function reset() {
    setIsRunning(false);
    setSecondsLeft(4);
    setPhase('Inhale');
  }

  return (
    <Card className="glass-card">
      <CardContent className="p-6 space-y-6">
        <div className="text-center">
          <h2 className="text-white font-bold text-lg">4-4-4-4 Box Breathing Cycle</h2>
          <p className="text-sm text-muted-foreground">Navy SEAL technique to abort acute stress in &lt;3 minutes.</p>
        </div>

        {/* Breathing Bubble */}
        <div className="flex justify-center">
          <div className="relative w-60 h-60 flex items-center justify-center">
            <div className="absolute rounded-full transition-all duration-[4000ms] ease-in-out"
              style={{
                width: `${bubbleSize}px`, height: `${bubbleSize}px`,
                backgroundColor: `${color}15`, border: `2px solid ${color}`,
              }} />
            <div className="relative z-10 text-center">
              <p className="text-3xl font-black" style={{ color }}>{phase}</p>
              <p className="text-5xl font-black text-white mt-1">{secondsLeft} Sec</p>
            </div>
          </div>
        </div>

        <div className="flex gap-2">
          <Button onClick={() => setIsRunning(!isRunning)}
            className="flex-1 gap-2"
            style={{ backgroundColor: isRunning ? '#F28F3B' : '#42E6A4' }}>
            {isRunning ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
            {isRunning ? 'Pause Breathing' : 'Start Deep Breathing'}
          </Button>
          {isRunning && (
            <Button variant="outline" size="icon" onClick={reset}>
              <RotateCcw className="w-4 h-4" />
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

// ==================== PMR Section ====================

function PMRSection() {
  const steps = [
    '1. Forehead: Wrinkle your forehead, squeezing tightly for 5 seconds... and release.',
    '2. Jaw: Clench your teeth and squeeze your jaw tightly for 5 seconds... exhale and release.',
    '3. Shoulders: Shrug your shoulders up toward your ears for 5 seconds... let them drop heavy.',
    '4. Chest & Back: Tighten chest muscles and pull shoulder blades back for 5 seconds... release.',
    '5. Arms & Hands: Make tight fists, squeezing forearms and biceps for 5 seconds... let them go limp.',
    '6. Thighs & Feet: Clench thigh muscles and curl toes downward for 5 seconds... release completely.',
  ];

  return (
    <div className="space-y-3">
      <div>
        <h2 className="text-white font-bold text-lg">Progressive Muscle Relaxation (PMR)</h2>
        <p className="text-sm text-muted-foreground">Systematically tensing and releasing muscle groups drains stored tension.</p>
      </div>
      {steps.map((step, i) => (
        <Card key={i} className="glass-card">
          <CardContent className="p-4">
            <p className="text-sm text-white leading-relaxed">{step}</p>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

// ==================== Grounding Section ====================

function GroundingSection() {
  const items = [
    '👀 5 things you can see around your study desk.',
    '🧎 4 things you can feel physically (chair, keyboard, floor, cold water).',
    '👂 3 things you can hear (traffic, clock ticking, birds).',
    '👃 2 things you can smell (tea, old textbooks, incense).',
    '👅 1 thing you can taste (water, mint, toothpaste).',
  ];

  return (
    <div className="space-y-3">
      <div>
        <h2 className="text-white font-bold text-lg">5-4-3-2-1 Sensory Grounding</h2>
        <p className="text-sm text-muted-foreground">Use when experiencing extreme overload, heart racing, or catastrophizing.</p>
      </div>
      {items.map((item, i) => (
        <div key={i} className="glass-card rounded-xl p-4">
          <p className="text-sm text-white leading-relaxed">{item}</p>
        </div>
      ))}
    </div>
  );
}

// ==================== Sleep Checklist ====================

function SleepChecklistSection() {
  const items = [
    'No textbooks or smartphone inside the bed area',
    'Finish caffeine / tea / coffee at least 6 hours before bedtime',
    'No backlit screens (laptops, phones) after 11 PM',
    'Dim your desk lights 30 minutes before winding down',
    'Aim for a consistent bedtime, even on weekends',
  ];
  const [checked, setChecked] = useState<Record<string, boolean>>({});

  return (
    <div className="space-y-3">
      <div>
        <h2 className="text-white font-bold text-lg">Sleep Hygiene Checklist</h2>
        <p className="text-sm text-muted-foreground">Inverted sleep cycles cause 40% memory retention loss. Check off what you completed today:</p>
      </div>
      {items.map(item => {
        const isChecked = checked[item] || false;
        return (
          <button key={item} onClick={() => setChecked(prev => ({ ...prev, [item]: !isChecked }))}
            className={`w-full flex items-center gap-3 rounded-xl p-4 border text-left transition-all ${
              isChecked
                ? 'bg-[var(--color-indigo)]/10 border-[var(--color-indigo)]'
                : 'glass-card'}`}>
            <Checkbox checked={isChecked} onCheckedChange={() => setChecked(prev => ({ ...prev, [item]: !isChecked }))} />
            <span className={`text-sm ${isChecked ? 'text-[var(--color-indigo)]' : 'text-white'}`}>{item}</span>
          </button>
        );
      })}
    </div>
  );
}
