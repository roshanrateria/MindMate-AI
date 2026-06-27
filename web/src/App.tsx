import { useState, useEffect } from 'react';
import { OnboardingScreen } from '@/screens/OnboardingScreen';
import { DashboardScreen } from '@/screens/DashboardScreen';
import { CheckInScreen } from '@/screens/CheckInScreen';
import { CompanionScreen } from '@/screens/CompanionScreen';
import { ToolkitScreen } from '@/screens/ToolkitScreen';
import { CrisisScreen } from '@/screens/CrisisScreen';
import { SettingsScreen } from '@/screens/SettingsScreen';
import { scheduleCheckInReminder } from '@/lib/gemini';
import type { OnboardingData } from '@/types';
import { LayoutDashboard, MessageCircle, Wrench, Settings, Brain } from 'lucide-react';

type Route = 'dashboard' | 'checkin' | 'companion' | 'toolkit' | 'crisis' | 'settings';

const NAV_ITEMS: { route: Route; label: string; icon: any }[] = [
  { route: 'dashboard', label: 'Home', icon: LayoutDashboard },
  { route: 'companion', label: 'Companion', icon: MessageCircle },
  { route: 'toolkit', label: 'Toolkit', icon: Wrench },
  { route: 'settings', label: 'Settings', icon: Settings },
];

function App() {
  const [onboarding, setOnboarding] = useState<OnboardingData | null>(null);
  const [route, setRoute] = useState<Route>('dashboard');
  const [prevRoute, setPrevRoute] = useState<Route>('dashboard');

  useEffect(() => {
    const saved = localStorage.getItem('mindmate_onboarding');
    if (saved) {
      try { setOnboarding(JSON.parse(saved)); } catch { /* ignore */ }
    }
    scheduleCheckInReminder();
  }, []);

  function handleOnboardingComplete(data: OnboardingData) {
    setOnboarding(data);
    localStorage.setItem('mindmate_onboarding', JSON.stringify(data));
  }

  function navigate(r: Route) {
    setPrevRoute(route);
    setRoute(r);
  }

  function handleReset() {
    localStorage.clear();
    indexedDB.deleteDatabase('mindmate-wellness');
    setOnboarding(null);
    setRoute('dashboard');
  }

  if (!onboarding?.completed) {
    return <OnboardingScreen onComplete={handleOnboardingComplete} />;
  }

  const showNav = route !== 'crisis' && route !== 'checkin';

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Status Bar Area */}
      <div className="safe-area-top" />

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto">
        <div className="max-w-lg mx-auto px-4 pt-4 pb-24">
          {route === 'dashboard' && (
            <DashboardScreen nickname={onboarding.nickname} onNavigate={r => navigate(r as Route)} />
          )}
          {route === 'checkin' && (
            <CheckInScreen
              examType={onboarding.examType}
              onComplete={() => navigate('dashboard')}
              onBack={() => navigate('dashboard')}
            />
          )}
          {route === 'companion' && (
            <CompanionScreen examType={onboarding.examType} onBack={() => navigate('dashboard')} />
          )}
          {route === 'toolkit' && (
            <ToolkitScreen onBack={() => navigate('dashboard')} />
          )}
          {route === 'crisis' && (
            <CrisisScreen onBack={() => navigate(prevRoute)} />
          )}
          {route === 'settings' && (
            <SettingsScreen onBack={() => navigate('dashboard')} onReset={handleReset} />
          )}
        </div>
      </main>

      {/* Bottom Navigation */}
      {showNav && (
        <nav className="fixed bottom-0 left-0 right-0 bg-[var(--color-surface)]/95 backdrop-blur-lg border-t border-[var(--color-glass-border)] z-50">
          <div className="max-w-lg mx-auto flex items-center justify-around py-2 pb-[max(0.5rem,env(safe-area-inset-bottom))]">
            {NAV_ITEMS.map(item => {
              const isActive = route === item.route;
              return (
                <button key={item.route} onClick={() => navigate(item.route)}
                  className={`flex flex-col items-center gap-0.5 px-4 py-1.5 rounded-xl transition-all ${
                    isActive ? 'text-[var(--color-indigo)]' : 'text-muted-foreground hover:text-white'}`}>
                  <div className={`p-1.5 rounded-xl transition-all ${isActive ? 'bg-[var(--color-indigo)]/15' : ''}`}>
                    <item.icon className="w-5 h-5" />
                  </div>
                  <span className="text-[10px] font-semibold">{item.label}</span>
                </button>
              );
            })}
          </div>
        </nav>
      )}
    </div>
  );
}

export default App;
