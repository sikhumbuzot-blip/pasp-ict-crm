/**
 * Sales CRM Common JavaScript Functions
 * Provides shared functionality across all pages
 */

// Global configuration
const CRM = {
    config: {
        alertTimeout: 5000,
        refreshInterval: 300000, // 5 minutes
        animationDuration: 300
    },
    
    // CSRF token management
    csrf: {
        token: null,
        header: null,
        
        init: function() {
            this.token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            this.header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        },
        
        getHeaders: function() {
            const headers = {
                'Content-Type': 'application/json'
            };
            if (this.token && this.header) {
                headers[this.header] = this.token;
            }
            return headers;
        }
    },
    
    // Alert management
    alerts: {
        show: function(message, type = 'info', timeout = CRM.config.alertTimeout) {
            const alertContainer = document.querySelector('.container-fluid');
            if (!alertContainer) return;
            
            const alertId = 'alert-' + Date.now();
            const alertHtml = `
                <div id="${alertId}" class="alert alert-${type} alert-dismissible fade show" role="alert">
                    <i class="bi bi-${this.getIcon(type)}"></i>
                    ${message}
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            `;
            
            alertContainer.insertAdjacentHTML('afterbegin', alertHtml);
            
            // Auto-hide after timeout
            if (timeout > 0) {
                setTimeout(() => {
                    const alert = document.getElementById(alertId);
                    if (alert) {
                        const bsAlert = new bootstrap.Alert(alert);
                        bsAlert.close();
                    }
                }, timeout);
            }
        },
        
        getIcon: function(type) {
            const icons = {
                'success': 'check-circle',
                'danger': 'exclamation-triangle',
                'warning': 'exclamation-triangle',
                'info': 'info-circle'
            };
            return icons[type] || 'info-circle';
        },
        
        hideAll: function() {
            const alerts = document.querySelectorAll('.alert');
            alerts.forEach(alert => {
                const bsAlert = new bootstrap.Alert(alert);
                bsAlert.close();
            });
        }
    },
    
    // Loading states
    loading: {
        show: function(element, text = 'Loading...') {
            if (!element) return;
            
            element.dataset.originalText = element.innerHTML;
            element.innerHTML = `<span class="spinner-border spinner-border-sm" role="status"></span> ${text}`;
            element.disabled = true;
            element.classList.add('loading');
        },
        
        hide: function(element) {
            if (!element) return;
            
            element.innerHTML = element.dataset.originalText || element.innerHTML;
            element.disabled = false;
            element.classList.remove('loading');
            delete element.dataset.originalText;
        }
    },
    
    // Form utilities
    forms: {
        validate: function(form) {
            if (!form) return false;
            
            const requiredFields = form.querySelectorAll('[required]');
            let isValid = true;
            
            requiredFields.forEach(field => {
                if (!field.value.trim()) {
                    field.classList.add('is-invalid');
                    isValid = false;
                } else {
                    field.classList.remove('is-invalid');
                }
            });
            
            return isValid;
        },
        
        serialize: function(form) {
            const formData = new FormData(form);
            const data = {};
            
            for (let [key, value] of formData.entries()) {
                if (data[key]) {
                    if (Array.isArray(data[key])) {
                        data[key].push(value);
                    } else {
                        data[key] = [data[key], value];
                    }
                } else {
                    data[key] = value;
                }
            }
            
            return data;
        },
        
        reset: function(form) {
            if (!form) return;
            
            form.reset();
            form.querySelectorAll('.is-invalid').forEach(field => {
                field.classList.remove('is-invalid');
            });
        }
    },
    
    // API utilities
    api: {
        get: async function(url) {
            try {
                const response = await fetch(url, {
                    method: 'GET',
                    headers: CRM.csrf.getHeaders()
                });
                
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                
                return await response.json();
            } catch (error) {
                console.error('API GET error:', error);
                CRM.alerts.show('Error fetching data. Please try again.', 'danger');
                throw error;
            }
        },
        
        post: async function(url, data) {
            try {
                const response = await fetch(url, {
                    method: 'POST',
                    headers: CRM.csrf.getHeaders(),
                    body: JSON.stringify(data)
                });
                
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                
                return await response.json();
            } catch (error) {
                console.error('API POST error:', error);
                CRM.alerts.show('Error saving data. Please try again.', 'danger');
                throw error;
            }
        },
        
        put: async function(url, data) {
            try {
                const response = await fetch(url, {
                    method: 'PUT',
                    headers: CRM.csrf.getHeaders(),
                    body: JSON.stringify(data)
                });
                
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                
                return await response.json();
            } catch (error) {
                console.error('API PUT error:', error);
                CRM.alerts.show('Error updating data. Please try again.', 'danger');
                throw error;
            }
        },
        
        delete: async function(url) {
            try {
                const response = await fetch(url, {
                    method: 'DELETE',
                    headers: CRM.csrf.getHeaders()
                });
                
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                
                return response.status === 204 ? null : await response.json();
            } catch (error) {
                console.error('API DELETE error:', error);
                CRM.alerts.show('Error deleting data. Please try again.', 'danger');
                throw error;
            }
        }
    },
    
    // Utility functions
    utils: {
        formatCurrency: function(amount, currency = 'USD') {
            return new Intl.NumberFormat('en-US', {
                style: 'currency',
                currency: currency
            }).format(amount);
        },
        
        formatDate: function(date, options = {}) {
            const defaultOptions = {
                year: 'numeric',
                month: 'short',
                day: 'numeric'
            };
            
            return new Intl.DateTimeFormat('en-US', { ...defaultOptions, ...options })
                .format(new Date(date));
        },
        
        formatDateTime: function(date) {
            return this.formatDate(date, {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        },
        
        debounce: function(func, wait) {
            let timeout;
            return function executedFunction(...args) {
                const later = () => {
                    clearTimeout(timeout);
                    func(...args);
                };
                clearTimeout(timeout);
                timeout = setTimeout(later, wait);
            };
        },
        
        throttle: function(func, limit) {
            let inThrottle;
            return function() {
                const args = arguments;
                const context = this;
                if (!inThrottle) {
                    func.apply(context, args);
                    inThrottle = true;
                    setTimeout(() => inThrottle = false, limit);
                }
            };
        },
        
        copyToClipboard: async function(text) {
            try {
                await navigator.clipboard.writeText(text);
                CRM.alerts.show('Copied to clipboard!', 'success', 2000);
            } catch (error) {
                console.error('Failed to copy to clipboard:', error);
                CRM.alerts.show('Failed to copy to clipboard', 'danger');
            }
        },
        
        downloadFile: function(data, filename, type = 'text/plain') {
            const blob = new Blob([data], { type });
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        }
    },
    
    // Dashboard utilities
    dashboard: {
        refresh: async function() {
            try {
                const data = await CRM.api.get('/dashboard/metrics');
                this.updateMetrics(data);
                this.updateTimestamp();
                CRM.alerts.show('Dashboard refreshed successfully', 'success', 2000);
            } catch (error) {
                console.error('Dashboard refresh failed:', error);
            }
        },
        
        updateMetrics: function(data) {
            // Update metric displays based on data
            Object.keys(data).forEach(key => {
                const elements = document.querySelectorAll(`[data-metric="${key}"]`);
                elements.forEach(element => {
                    if (element.tagName === 'INPUT') {
                        element.value = data[key];
                    } else {
                        element.textContent = data[key];
                    }
                });
            });
        },
        
        updateTimestamp: function() {
            const timestampElements = document.querySelectorAll('[id*="lastUpdated"]');
            const now = new Date().toLocaleTimeString();
            timestampElements.forEach(element => {
                element.textContent = `Last updated: ${now}`;
            });
        },
        
        startAutoRefresh: function(interval = CRM.config.refreshInterval) {
            setInterval(() => {
                this.refresh();
            }, interval);
        }
    },
    
    // Chart utilities
    charts: {
        defaultOptions: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                }
            }
        },
        
        colors: {
            primary: '#667eea',
            secondary: '#764ba2',
            success: '#28a745',
            danger: '#dc3545',
            warning: '#ffc107',
            info: '#17a2b8'
        },
        
        createLineChart: function(ctx, data, options = {}) {
            return new Chart(ctx, {
                type: 'line',
                data: data,
                options: { ...this.defaultOptions, ...options }
            });
        },
        
        createBarChart: function(ctx, data, options = {}) {
            return new Chart(ctx, {
                type: 'bar',
                data: data,
                options: { ...this.defaultOptions, ...options }
            });
        },
        
        createDoughnutChart: function(ctx, data, options = {}) {
            return new Chart(ctx, {
                type: 'doughnut',
                data: data,
                options: { ...this.defaultOptions, ...options }
            });
        }
    },
    
    // Initialize the CRM system
    init: function() {
        this.csrf.init();
        this.setupEventListeners();
        this.autoHideAlerts();
        
        // Initialize tooltips and popovers
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
        
        const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
        popoverTriggerList.map(function (popoverTriggerEl) {
            return new bootstrap.Popover(popoverTriggerEl);
        });
        
        console.log('Sales CRM initialized successfully');
    },
    
    setupEventListeners: function() {
        // Global form validation
        document.addEventListener('submit', function(e) {
            const form = e.target;
            if (form.classList.contains('needs-validation')) {
                if (!CRM.forms.validate(form)) {
                    e.preventDefault();
                    e.stopPropagation();
                }
                form.classList.add('was-validated');
            }
        });
        
        // Global loading states for buttons
        document.addEventListener('click', function(e) {
            const button = e.target.closest('button[type="submit"]');
            if (button && !button.classList.contains('no-loading')) {
                CRM.loading.show(button);
            }
        });
        
        // Auto-save forms (if marked with data-auto-save)
        document.addEventListener('input', CRM.utils.debounce(function(e) {
            const form = e.target.closest('form[data-auto-save]');
            if (form) {
                CRM.autoSaveForm(form);
            }
        }, 1000));
        
        // Keyboard shortcuts
        document.addEventListener('keydown', function(e) {
            // Ctrl/Cmd + R for refresh
            if ((e.ctrlKey || e.metaKey) && e.key === 'r' && e.target.closest('.dashboard')) {
                e.preventDefault();
                CRM.dashboard.refresh();
            }
            
            // Escape to close modals
            if (e.key === 'Escape') {
                const modals = document.querySelectorAll('.modal.show');
                modals.forEach(modal => {
                    const bsModal = bootstrap.Modal.getInstance(modal);
                    if (bsModal) bsModal.hide();
                });
            }
        });
    },
    
    autoHideAlerts: function() {
        setTimeout(() => {
            const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');
            alerts.forEach(alert => {
                const bsAlert = new bootstrap.Alert(alert);
                if (bsAlert) bsAlert.close();
            });
        }, this.config.alertTimeout);
    },
    
    autoSaveForm: async function(form) {
        try {
            const data = this.forms.serialize(form);
            const url = form.getAttribute('data-auto-save-url') || form.action;
            
            await this.api.post(url, data);
            
            // Show subtle save indicator
            const saveIndicator = form.querySelector('.save-indicator');
            if (saveIndicator) {
                saveIndicator.textContent = 'Saved';
                saveIndicator.classList.add('text-success');
                setTimeout(() => {
                    saveIndicator.textContent = '';
                    saveIndicator.classList.remove('text-success');
                }, 2000);
            }
        } catch (error) {
            console.error('Auto-save failed:', error);
        }
    }
};

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    CRM.init();
});

// Export for use in other scripts
window.CRM = CRM;