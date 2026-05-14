/**
 * 将文本中的 @username 转换为可点击的 HTML 链接。
 * 匹配规则：@ 后跟中文/英文/数字/下划线/连字符，1-30 字符。
 */
const MENTION_RE = /@([\w一-龥-]{1,30})/g

export function renderMentions(text: string): string {
  if (!text) return ''
  return text.replace(MENTION_RE, (_match, name) => {
    return `<a href="/search?q=${encodeURIComponent('@' + name)}" class="mention-link">@${name}</a>`
  })
}
