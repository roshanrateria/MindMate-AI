import { openDB, type DBSchema, type IDBPDatabase } from 'idb';
import type { CheckIn, AIAnalysis, ChatMessage } from '../types';

interface MindMateDB extends DBSchema {
  checkIns: {
    key: number;
    value: CheckIn;
    indexes: { 'by-timestamp': number };
  };
  analyses: {
    key: number;
    value: AIAnalysis;
    indexes: { 'by-checkInId': number; 'by-timestamp': number };
  };
  chatMessages: {
    key: number;
    value: ChatMessage;
    indexes: { 'by-timestamp': number };
  };
}

let dbPromise: Promise<IDBPDatabase<MindMateDB>> | null = null;

function getDB() {
  if (!dbPromise) {
    dbPromise = openDB<MindMateDB>('mindmate-wellness', 1, {
      upgrade(db) {
        const checkInStore = db.createObjectStore('checkIns', { keyPath: 'id', autoIncrement: true });
        checkInStore.createIndex('by-timestamp', 'timestamp');

        const analysisStore = db.createObjectStore('analyses', { keyPath: 'id', autoIncrement: true });
        analysisStore.createIndex('by-checkInId', 'checkInId');
        analysisStore.createIndex('by-timestamp', 'timestamp');

        const chatStore = db.createObjectStore('chatMessages', { keyPath: 'id', autoIncrement: true });
        chatStore.createIndex('by-timestamp', 'timestamp');
      },
    });
  }
  return dbPromise;
}

// ==================== Check-In Operations ====================

export async function getAllCheckIns(): Promise<CheckIn[]> {
  const db = await getDB();
  const all = await db.getAll('checkIns');
  return all.sort((a, b) => b.timestamp - a.timestamp);
}

export async function insertCheckIn(checkIn: Omit<CheckIn, 'id'>): Promise<number> {
  const db = await getDB();
  return await db.add('checkIns', checkIn as CheckIn);
}

export async function deleteCheckIn(id: number): Promise<void> {
  const db = await getDB();
  await db.delete('checkIns', id);
}

// ==================== Analysis Operations ====================

export async function getAllAnalyses(): Promise<AIAnalysis[]> {
  const db = await getDB();
  const all = await db.getAll('analyses');
  return all.sort((a, b) => b.timestamp - a.timestamp);
}

export async function insertAnalysis(analysis: Omit<AIAnalysis, 'id'>): Promise<number> {
  const db = await getDB();
  return await db.add('analyses', analysis as AIAnalysis);
}

// ==================== Chat Operations ====================

export async function getChatMessages(): Promise<ChatMessage[]> {
  const db = await getDB();
  const all = await db.getAll('chatMessages');
  return all.sort((a, b) => a.timestamp - b.timestamp);
}

export async function insertChatMessage(msg: Omit<ChatMessage, 'id'>): Promise<number> {
  const db = await getDB();
  return await db.add('chatMessages', msg as ChatMessage);
}

export async function clearChatHistory(): Promise<void> {
  const db = await getDB();
  await db.clear('chatMessages');
}
