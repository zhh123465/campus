import { request } from './request';
import type { ResourceVO } from '@/types/resource';

export async function uploadResource(
  file: File,
  data: Record<string, any>,
): Promise<ResourceVO> {
  const formData = new FormData();
  formData.append('file', file);
  for (const [key, value] of Object.entries(data)) {
    if (value !== undefined && value !== null) {
      if (Array.isArray(value)) {
        value.forEach((v) => formData.append(key, v));
      } else {
        formData.append(key, String(value));
      }
    }
  }
  const res = await request<ResourceVO>({
    method: 'POST',
    url: '/resources',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return res.data;
}

export async function getResources(params: {
  spaceId?: number;
  college?: string;
  major?: string;
  course?: string;
  cursor?: number;
  limit?: number;
}): Promise<ResourceVO[]> {
  const res = await request<ResourceVO[]>({ method: 'GET', url: '/resources', params });
  return res.data;
}

export async function getResourceById(id: number): Promise<ResourceVO> {
  const res = await request<ResourceVO>({ method: 'GET', url: `/resources/${id}` });
  return res.data;
}

export function getDownloadUrl(id: number): string {
  const token = localStorage.getItem('token');
  const base = import.meta.env.VITE_API_BASE || '/api/v1';
  return `${base}/resources/${id}/download?token=${token}`;
}

export async function deleteResource(id: number): Promise<void> {
  await request({ method: 'DELETE', url: `/resources/${id}` });
}
