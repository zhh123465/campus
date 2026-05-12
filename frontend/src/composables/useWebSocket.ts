import { ref, onMounted, onUnmounted } from 'vue';

export interface NotifyEvent {
  type: string;
  title: string;
  content: string;
}

export function useWebSocket(onNotify?: (event: NotifyEvent) => void) {
  const connected = ref(false);
  let ws: WebSocket | null = null;
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  function connect() {
    const token = localStorage.getItem('token');
    if (!token) return;

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const url = `${protocol}//${location.host}/ws/notify?token=${token}`;

    ws = new WebSocket(url);

    ws.onopen = () => {
      connected.value = true;
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as NotifyEvent;
        onNotify?.(data);
      } catch {
        // ignore malformed messages
      }
    };

    ws.onclose = () => {
      connected.value = false;
      ws = null;
      reconnectTimer = setTimeout(connect, 5000);
    };

    ws.onerror = () => {
      ws?.close();
    };
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    if (ws) {
      ws.onclose = null;
      ws.close();
      ws = null;
    }
    connected.value = false;
  }

  onMounted(connect);
  onUnmounted(disconnect);

  return { connected, disconnect };
}
