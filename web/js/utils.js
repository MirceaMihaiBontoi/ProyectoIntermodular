/**
 * Shared utility functions
 */

export const utils = {
    escapeHTML(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    },

    log(message, type = 'info') {
        const icons = { info: 'ℹ️', success: '✅', error: '❌', warn: '⚠️' };
        if (window.location.hostname === 'localhost') {
            console[type === 'success' ? 'log' : type](`${icons[type] || ''} ${message}`);
        }
    }
};
