import { WS_URL } from './config.js';
import { utils } from './utils.js';

/**
 * WebSocket Management Service
 */
export class WebSocketService {
    constructor(onRefresh) {
        this.onRefresh = onRefresh;
        this.reconnectAttempts = 0;
        this.socket = null;
    }

    init() {
        this.socket = new WebSocket(WS_URL);
        
        this.socket.onopen = () => {
            utils.log('WebSocket connected', 'success');
            this.reconnectAttempts = 0;
        };

        this.socket.onmessage = (event) => {
            if (event.data === 'refresh') {
                utils.log('Data change detected, refreshing...');
                if (this.onRefresh) this.onRefresh();
            }
        };

        this.socket.onclose = () => {
            this.reconnectAttempts++;
            const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
            utils.log(`WebSocket closed. Reconnecting in ${delay/1000}s (Attempt ${this.reconnectAttempts})...`, 'warn');
            setTimeout(() => this.init(), delay);
        };

        this.socket.onerror = () => {
            utils.log('WebSocket Error', 'error');
        };
    }
}
