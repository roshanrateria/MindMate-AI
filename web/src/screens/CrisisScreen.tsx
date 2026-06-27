import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { ArrowLeft, AlertTriangle, Phone, Info, Shield, Heart } from 'lucide-react';

interface CrisisScreenProps {
  onBack: () => void;
}

const HELPLINES = [
  { name: 'Tele-MANAS (Govt Indian Helpline)', phone: '14416', hours: '24x7', languages: '20+ Regional languages' },
  { name: 'Vandrevala Foundation Helpline', phone: '18602662345', hours: '24x7', languages: 'Hindi, English, 8 Regional' },
  { name: 'Manodarpan (MHRD Student Helpline)', phone: '8448440632', hours: '8 AM to 8 PM', languages: 'Hindi, English' },
  { name: 'iCALL (TATA Institute of Social Sci.)', phone: '02225521111', hours: 'Mon-Sat, 8 AM - 10 PM', languages: 'English, Hindi' },
  { name: 'AASRA Support', phone: '9820466726', hours: '24x7', languages: 'Hindi, English' },
];

export function CrisisScreen({ onBack }: CrisisScreenProps) {
  return (
    <div className="space-y-5 pb-6">
      <button onClick={onBack} className="flex items-center gap-2 text-muted-foreground hover:text-white transition-colors">
        <ArrowLeft className="w-4 h-4" /> Back
      </button>

      {/* Warning Header */}
      <Card className="border-[var(--color-red-crisis)]/30 bg-[var(--color-red-crisis)]/10">
        <CardContent className="p-6 text-center space-y-3">
          <AlertTriangle className="w-12 h-12 text-[var(--color-red-crisis)] mx-auto" />
          <h1 className="text-2xl font-black text-white">You are not alone.</h1>
          <p className="text-sm text-white/80 leading-relaxed">
            We see you're going through something really hard right now. Exam stress can feel crushing,
            but please remember: your life is immensely precious. Your target rank doesn't define your future.
          </p>
        </CardContent>
      </Card>

      {/* Helplines */}
      <div>
        <h2 className="text-white font-bold text-lg">Confidential, Free, 24/7 Human Help</h2>
        <p className="text-sm text-muted-foreground mb-3">Tap to speak with a warm counselor who understands student stress:</p>

        {HELPLINES.map(h => (
          <Card key={h.phone} className="glass-card mb-3">
            <CardContent className="p-4 flex items-center gap-3">
              <div className="flex-1 space-y-1">
                <h3 className="text-white font-bold text-sm">{h.name}</h3>
                <div className="flex items-center gap-1.5 text-[var(--color-indigo)]">
                  <Phone className="w-3.5 h-3.5" />
                  <span className="text-sm font-semibold">{h.phone}</span>
                </div>
                <p className="text-xs text-muted-foreground">Hours: {h.hours} | Lang: {h.languages}</p>
              </div>
              <a href={`tel:${h.phone}`}
                className="w-12 h-12 rounded-full bg-[var(--color-indigo)] flex items-center justify-center shadow-lg shadow-[var(--color-indigo)]/30 hover:scale-105 transition-transform">
                <Phone className="w-5 h-5 text-white" />
              </a>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Coping Statements */}
      <Card className="glass-card">
        <CardContent className="p-5 space-y-3">
          <h3 className="text-white font-bold flex items-center gap-2">
            <Heart className="w-5 h-5 text-[var(--color-lavender)]" />
            Grounding coping statements:
          </h3>
          {[
            '"This mock score measures memory recall in a speed run; it does not measure my core capacity or future career paths."',
            '"I am studying away from home in a high-pressure coaching environment. Missing home is a strength, not a weakness."',
            '"An exam is just one gate. There are multiple, beautiful alternate pathways to success and happiness."',
          ].map((text, i) => (
            <p key={i} className="text-sm text-white/80 leading-relaxed">• {text}</p>
          ))}
        </CardContent>
      </Card>

      {/* Plan B */}
      <Card className="glass-card">
        <CardContent className="p-5 space-y-3">
          <div className="flex items-center gap-2">
            <Info className="w-5 h-5 text-[var(--color-teal)]" />
            <h3 className="text-white font-bold">De-escalating the Fear of Failure</h3>
          </div>
          <p className="text-sm text-muted-foreground leading-relaxed">
            Research proves that having a secondary plan increases performance and reduces chronic stress.
            You don't have to study with a knife to your neck. Take a piece of paper right now and sketch out
            3 alternate things you would love to do if this exam did not exist. Discuss this list with a trusted
            friend or our AI Companion.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
