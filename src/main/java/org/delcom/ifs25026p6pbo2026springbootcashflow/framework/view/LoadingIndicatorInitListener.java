package org.delcom.ifs25026p6pbo2026springbootcashflow.framework.view;

import com.vaadin.flow.server.VaadinServiceInitListener;

public class LoadingIndicatorInitListener implements VaadinServiceInitListener {
    private static final String LOADING_INDICATOR_SCRIPT = """
            if (!document.getElementById('cashflow-loading')) {
              document.documentElement.setAttribute('lang', 'id');
              const indicator = document.createElement('div');
              indicator.id = 'cashflow-loading';
              indicator.textContent = 'Memuat...';
              indicator.style.cssText = 'display:none;position:fixed;top:0;left:0;right:0;z-index:9999;'
                + 'height:3px;background:#2563eb;color:transparent;box-shadow:0 0 8px #2563eb;';
              document.body.appendChild(indicator);
              document.addEventListener('vaadin-router-location-changed', () => {
                indicator.style.display = 'block';
                window.setTimeout(() => { indicator.style.display = 'none'; }, 700);
              });
              const setConnectionLandmark = () => {
                const connection = document.querySelector('vaadin-connection-indicator');
                if (!connection) return;
                connection.setAttribute('role', 'region');
                connection.setAttribute('aria-label', 'Status koneksi');
              };
              setConnectionLandmark();
              document.addEventListener('vaadin-router-location-changed', setConnectionLandmark);
            }
            """;

    @Override
    public void serviceInit(com.vaadin.flow.server.ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent ->
                uiEvent.getUI().getPage().executeJs(LOADING_INDICATOR_SCRIPT));
    }
}
