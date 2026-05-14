import { describe, expect, it } from 'vitest';
import { renderMentions } from './mention';

function mentionLink(name: string): string {
  return `<a href="/search?q=${encodeURIComponent('@' + name)}" class="mention-link">@${name}</a>`;
}

describe('renderMentions', () => {
  it('returns an empty string for empty input', () => {
    expect(renderMentions('')).toBe('');
  });

  it('leaves text without mentions unchanged', () => {
    expect(renderMentions('今天没有提到任何人')).toBe('今天没有提到任何人');
  });

  it('renders supported mention names as search links', () => {
    expect(renderMentions('请 @alice 和 @张三-2024 看一下')).toBe(
      `请 ${mentionLink('alice')} 和 ${mentionLink('张三-2024')} 看一下`,
    );
  });
});
