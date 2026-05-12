export interface ReportVO {
  id: number;
  reporterId: number;
  reporterName: string;
  targetType: string;
  targetId: number;
  reason: string;
  description: string | null;
  status: number;
  handlerId: number | null;
  handlerName: string | null;
  handleNote: string | null;
  createdAt: string;
  handledAt: string | null;
}
