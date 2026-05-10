import { request } from './request';
import type { CommentVO, CreateCommentRequest } from '@/types/post';

export async function createComment(data: CreateCommentRequest): Promise<CommentVO> {
  const res = await request<CommentVO>({
    method: 'POST',
    url: `/posts/${data.postId}/comments`,
    data,
  });
  return res.data;
}

export async function getComments(postId: number, cursor?: number, limit = 20): Promise<CommentVO[]> {
  const res = await request<CommentVO[]>({
    method: 'GET',
    url: `/posts/${postId}/comments`,
    params: { cursor, limit },
  });
  return res.data;
}

export async function deleteComment(id: number): Promise<void> {
  await request({ method: 'DELETE', url: `/comments/${id}` });
}
