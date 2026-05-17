/**
 * 将文本中的 @username 转换为可点击的 HTML 链接。
 * 匹配规则：@ 后跟中文/英文/数字/下划线/连字符，1-30 字符。
 */
const MENTION_RE = /@([\w一-龥-]{1,30})/g;

export interface MentionSegment {
  text: string;
  mention?: string;
}

export function parseMentions(text: string): MentionSegment[] {
  if (!text) return [];

  MENTION_RE.lastIndex = 0;
  const segments: MentionSegment[] = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = MENTION_RE.exec(text)) !== null) {
    if (match.index > lastIndex) {
      segments.push({ text: text.slice(lastIndex, match.index) });
    }
    segments.push({ text: match[0], mention: match[1] });
    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < text.length) {
    segments.push({ text: text.slice(lastIndex) });
  }

  return segments;
}

export function renderMentions(text: string): string {
  if (!text) return '';
  return text.replace(MENTION_RE, (_match, name) => {
    return `<a href="/search?q=${encodeURIComponent('@' + name)}" class="mention-link">@${name}</a>`;
  });
}
