export interface UserBrief {
  id: number;
  nickname: string;
  avatarUrl: string;
  email: string;
}

export interface SpaceVO {
  id: number;
  ownerId: number;
  owner: UserBrief | null;
  name: string;
  description: string;
  category: string;
  visibility: string;
  memberCount: number;
  postCount: number;
  status: number;
  isMember: boolean;
  memberRole: string | null;
  sensitiveWords?: string;
  postNotice?: string;
  createdAt: string;
}

export interface SpaceMemberVO {
  id: number;
  spaceId: number;
  userId: number;
  user: UserBrief | null;
  role: string;
  status: number;
  joinedAt: string;
}

export interface CreateSpaceRequest {
  name: string;
  description?: string;
  category: string;
  visibility?: string;
}

export interface UpdateSpaceRequest {
  name?: string;
  description?: string;
  visibility?: string;
  sensitiveWords?: string;
  postNotice?: string;
}
