import { request } from './request';

export type AiWorkspacePage<T> = {
  items: T[];
  total: number;
};

export type AiKnowledgeBaseItem = {
  id: string;
  name: string;
  description?: string;
  category?: string;
  type?: string;
  documentCount?: number;
  vectorCount?: number;
  storageBytes?: number;
  updatedAt?: string;
  owner?: string;
  isMine?: boolean;
  isFavorite?: boolean;
};

export type AiKnowledgeDocumentItem = {
  id: string;
  fileName: string;
  fileType?: string;
  status?: string;
  chunkCount?: number;
  storageBytes?: number;
  indexedAt?: string;
  errorMessage?: string;
  createdAt?: string;
};

export type AiConversationItem = {
  id: string;
  title: string;
  knowledgeBaseIds?: string[];
  model?: string;
  updatedAt?: string;
};

export type AiWorkspaceMessage = {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  citations?: Array<{
    title: string;
    snippet: string;
    knowledgeBaseId?: string;
    documentId?: string;
    chunkId?: string;
    score?: number;
  }>;
  createdAt?: string;
};

export async function listKnowledgeBases(): Promise<AiWorkspacePage<AiKnowledgeBaseItem>> {
  const res = await request<AiWorkspacePage<AiKnowledgeBaseItem>>({
    method: 'GET',
    url: '/ai/knowledge-bases',
    params: { page: 1, pageSize: 100, sort: 'recent' },
  });
  return res.data;
}

export async function createKnowledgeBase(data: {
  name: string;
  description?: string;
  category?: string;
  type?: string;
  visibility?: string;
}): Promise<AiKnowledgeBaseItem> {
  const res = await request<AiKnowledgeBaseItem>({ method: 'POST', url: '/ai/knowledge-bases', data });
  return res.data;
}

export async function listKnowledgeDocuments(knowledgeBaseId: string): Promise<AiKnowledgeDocumentItem[]> {
  const res = await request<AiKnowledgeDocumentItem[]>({
    method: 'GET',
    url: `/ai/knowledge-bases/${knowledgeBaseId}/documents`,
  });
  return res.data ?? [];
}

export async function uploadKnowledgeDocuments(knowledgeBaseId: string, files: File[], tags?: string): Promise<{
  taskId: string;
  status: string;
  uploaded: number;
  failed: number;
  documentIds: string[];
  errorMessage?: string;
}> {
  const form = new FormData();
  files.forEach((file) => form.append('files', file));
  if (tags) form.append('tags', tags);
  form.append('parseMode', 'auto');
  const res = await request({
    method: 'POST',
    url: `/ai/knowledge-bases/${knowledgeBaseId}/documents`,
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return res.data as {
    taskId: string;
    status: string;
    uploaded: number;
    failed: number;
    documentIds: string[];
    errorMessage?: string;
  };
}

export async function createAiConversation(data: {
  title: string;
  knowledgeBaseIds?: string[];
  model?: string;
}): Promise<AiConversationItem> {
  const res = await request<AiConversationItem>({ method: 'POST', url: '/ai/conversations', data });
  return res.data;
}

export async function sendAiWorkspaceMessage(conversationId: string, content: string, model?: string): Promise<{
  userMessage: AiWorkspaceMessage;
  assistantMessage: AiWorkspaceMessage;
}> {
  const res = await request<{ userMessage: AiWorkspaceMessage; assistantMessage: AiWorkspaceMessage }>({
    method: 'POST',
    url: `/ai/conversations/${conversationId}/messages`,
    data: { content, model },
  });
  return res.data;
}
