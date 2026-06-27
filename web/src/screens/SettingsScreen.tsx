import { useState, useEffect } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { ArrowLeft, Key, Bell, Database, Trash2, CheckCircle, Brain, ExternalLink } from 'lucide-react';
import { setApiKey, hasApiKey, requestNotificationPermission } from '@/lib/gemini';

interface SettingsScreenProps {
  onBack: () => void;
  onReset: () => void;
}

export function SettingsScreen({ onBack, onReset }: SettingsScreenProps) {
  const [apiKeyInput, setApiKeyInput] = useState('');
  const [isKeySaved, setIsKeySaved] = useState(hasApiKey());
  const [notifEnabled, setNotifEnabled] = useState(false);

  useEffect(() => {
    if ('Notification' in window) {
      setNotifEnabled(Notification.permission === 'granted');
    }
  }, []);

  function saveKey() {
    if (apiKeyInput.trim()) {
      setApiKey(apiKeyInput.trim());
      setIsKeySaved(true);
      setApiKeyInput('');
    }
  }

  async function enableNotifications() {
    const granted = await requestNotificationPermission();
    setNotifEnabled(granted);
  }

  return (
    <div className="space-y-5 pb-6">
      <div className="flex items-center gap-3">
        <button onClick={onBack} className="text-muted-foreground hover:text-white transition-colors">
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-xl font-bold text-white">Settings</h1>
          <p className="text-xs text-muted-foreground">Manage privacy & AI configuration</p>
        </div>
      </div>

      {/* API Key */}
      <Card className="glass-card">
        <CardContent className="p-5 space-y-3">
          <div className="flex items-center gap-2">
            <Key className="w-5 h-5 text-[var(--color-indigo)]" />
            <h3 className="text-white font-bold">Gemini API Key</h3>
          </div>
          <p className="text-xs text-muted-foreground">
            Stored locally in your browser. Required for AI analysis and companion chat.
          </p>
          {isKeySaved && (
            <div className="flex items-center gap-2 text-[var(--color-green-resilient)]">
              <CheckCircle className="w-4 h-4" />
              <span className="text-sm font-medium">API Key configured ✓</span>
            </div>
          )}
          <div className="flex gap-2">
            <input
              type="password"
              value={apiKeyInput}
              onChange={e => setApiKeyInput(e.target.value)}
              placeholder={isKeySaved ? 'Enter new key to update...' : 'Paste your Gemini API key'}
              className="flex-1 bg-background border border-[var(--color-glass-border)] rounded-xl px-4 py-3 text-white text-sm placeholder:text-muted-foreground focus:outline-none focus:border-[var(--color-indigo)]"
            />
            <Button onClick={saveKey} disabled={!apiKeyInput.trim()}>Save</Button>
          </div>
          <a href="https://aistudio.google.com/app/apikey" target="_blank" rel="noopener noreferrer"
            className="flex items-center gap-1.5 text-xs text-[var(--color-indigo)] hover:underline">
            <ExternalLink className="w-3 h-3" /> Get free API key from Google AI Studio
          </a>
        </CardContent>
      </Card>

      {/* Notifications */}
      <Card className="glass-card">
        <CardContent className="p-5 space-y-3">
          <div className="flex items-center gap-2">
            <Bell className="w-5 h-5 text-[var(--color-lavender)]" />
            <h3 className="text-white font-bold">Daily Reminders</h3>
          </div>
          <p className="text-xs text-muted-foreground">
            Get a gentle reminder at 8 PM daily to log your wellness check-in.
          </p>
          {notifEnabled ? (
            <div className="flex items-center gap-2 text-[var(--color-green-resilient)]">
              <CheckCircle className="w-4 h-4" />
              <span className="text-sm font-medium">Notifications enabled ✓</span>
            </div>
          ) : (
            <Button onClick={enableNotifications} variant="outline" className="gap-2">
              <Bell className="w-4 h-4" /> Enable Push Notifications
            </Button>
          )}
        </CardContent>
      </Card>

      {/* Data */}
      <Card className="glass-card">
        <CardContent className="p-5 space-y-3">
          <div className="flex items-center gap-2">
            <Database className="w-5 h-5 text-[var(--color-teal)]" />
            <h3 className="text-white font-bold">Data & Privacy</h3>
          </div>
          <p className="text-xs text-muted-foreground">
            All your data is stored locally in your browser's IndexedDB. Nothing is sent to our servers.
            Only journal analysis uses the Gemini API (encrypted in transit).
          </p>
          <Button onClick={onReset} variant="destructive" className="gap-2">
            <Trash2 className="w-4 h-4" /> Reset All Data & Restart
          </Button>
        </CardContent>
      </Card>

      {/* About */}
      <Card className="glass-card">
        <CardContent className="p-5 space-y-2">
          <div className="flex items-center gap-2">
            <Brain className="w-5 h-5 text-[var(--color-indigo)]" />
            <h3 className="text-white font-bold">About MindMate AI</h3>
          </div>
          <p className="text-xs text-muted-foreground leading-relaxed">
            MindMate AI is a free, privacy-first mental wellness companion designed for Indian competitive exam aspirants.
            Built with CBT-based wellness tracking, AI-powered empathy support, and evidence-based coping tools.
          </p>
          <p className="text-xs text-muted-foreground/60">Version 2.0 (Web) • Powered by Gemini AI</p>
        </CardContent>
      </Card>
    </div>
  );
}
