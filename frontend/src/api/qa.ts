import { request } from './request';
import type { QaQuestionVO } from '@/types/qa';

export async function getQaInfo(postId: number): Promise<QaQuestionVO | null> {
  try {
    const res = await request<QaQuestionVO>({ method: 'GET', url: `/qa/${postId}` });
    return res.data;
  } catch {
    return null;
  }
}

export async function acceptAnswer(postId: number, commentId: number): Promise<QaQuestionVO> {
  const res = await request<QaQuestionVO>({ method: 'POST', url: `/qa/${postId}/accept/${commentId}` });
  return res.data;
}
