// URL de la API backend (Spring Boot en puerto 8080)
const API_BASE_URL = 'http://localhost:8080/api';

// Espera a que el DOM esté completamente cargado antes de ejecutar el codigo
document.addEventListener('DOMContentLoaded', function() {
    
    // Elementos del DOM
    const travelForm = document.getElementById('travelForm');
    const daysSlider = document.getElementById('days');
    const daysValue = document.getElementById('daysValue');
    const budgetSlider = document.getElementById('budget');
    const budgetValue = document.getElementById('budgetValue');
    const generateBtn = document.getElementById('generateBtn');
    const loadingDiv = document.getElementById('loading');
    const errorDiv = document.getElementById('error');
    const resultsDiv = document.getElementById('results');
    const planSummaryDiv = document.getElementById('planSummary');
    const downloadPdfBtn = document.getElementById('downloadPdfBtn');

    let currentTravelPlan = null;

    // Actualiza el valor mostrado del slider de dias
    function updateDaysValue() {
        daysValue.textContent = daysSlider.value;
    }

    // Actualiza el valor mostrado del slider de presupuesto
    function updateBudgetValue() {
        budgetValue.textContent = budgetSlider.value;
    }

    // Muestra un mensaje de error en el interfaz
    function showError(message) {
        errorDiv.textContent = message;
        errorDiv.classList.remove('hidden');
        setTimeout(() => {
            errorDiv.classList.add('hidden');
        }, 5000);
    }

    // Muestra el loader y oculta resultados y errores
    function showLoading() {
        loadingDiv.classList.remove('hidden');
        resultsDiv.classList.add('hidden');
        errorDiv.classList.add('hidden');
        generateBtn.disabled = true;
    }

    // Oculta el loader y habilita el boton
    function hideLoading() {
        loadingDiv.classList.add('hidden');
        generateBtn.disabled = false;
    }

    // Muestra el plan de viaje en el interfaz
    function displayTravelPlan(plan) {
        let html = `<h3>${plan.destination || 'Tu destino'}</h3>`;
        html += `<p><strong>Dias:</strong> ${plan.totalDays}</p>`;
        html += `<p><strong>Presupuesto total:</strong> ${plan.totalBudget} €</p>`;
        
        if (plan.itinerary && plan.itinerary.length > 0) {
            html += `<h3>Itinerario diario</h3>`;
            plan.itinerary.forEach(day => {
                html += `<div class="day-card">`;
                html += `<h4>Dia ${day.day}</h4>`;
                if (day.morning) {
                    html += `<p><strong>Mañana:</strong> ${day.morning}</p>`;
                }
                if (day.lunch) {
                    html += `<p><strong>Comida (mediodia):</strong> ${day.lunch}</p>`;
                }
                if (day.afternoon) {
                    html += `<p><strong>Tarde:</strong> ${day.afternoon}</p>`;
                }
                html += `</div>`;
            });
        }
        
        planSummaryDiv.innerHTML = html;
        resultsDiv.classList.remove('hidden');
    }

    // Funcion principal que envia la peticion al backend para generar el plan
    async function generateTravelPlan(requestData) {
        try {
            const response = await fetch(`${API_BASE_URL}/plan/generate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Error ${response.status}: ${errorText}`);
            }

            const data = await response.json();
            currentTravelPlan = data;
            displayTravelPlan(data);
            return data;
        } catch (error) {
            console.error('Error:', error);
            showError('No se pudo generar el plan de viaje. Asegurate de que el backend esta ejecutandose en el puerto 8080');
            throw error;
        }
    }

    // Funcion para descargar el plan en PDF
    async function downloadPdf() {
        if (!currentTravelPlan) {
            showError('No hay un plan generado para descargar');
            return;
        }

        try {
            showLoading();
            const response = await fetch(`${API_BASE_URL}/pdf/generate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(currentTravelPlan)
            });

            if (!response.ok) {
                throw new Error('Error al generar el PDF');
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `travel_plan_${currentTravelPlan.destination || 'europe'}.pdf`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error('Error:', error);
            showError('No se pudo generar el PDF');
        } finally {
            hideLoading();
        }
    }

    // Eventos de los sliders
    daysSlider.addEventListener('input', updateDaysValue);
    budgetSlider.addEventListener('input', updateBudgetValue);

    // Evento de envio del formulario
    travelForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        const countriesSelect = document.getElementById('countries');
        const selectedCountries = Array.from(countriesSelect.selectedOptions).map(opt => opt.value);
        
        const city = document.getElementById('city').value;
        const days = parseInt(daysSlider.value);
        const budget = parseInt(budgetSlider.value);

        if (!city) {
            showError('Por favor selecciona una ciudad');
            return;
        }

        if (selectedCountries.length === 0) {
            showError('Por favor selecciona al menos un pais');
            return;
        }

        const requestData = {
            countries: selectedCountries,
            city: city,
            days: days,
            budget: budget
        };

        showLoading();
        await generateTravelPlan(requestData);
        hideLoading();
    });

    // Evento del boton de descarga de PDF
    if (downloadPdfBtn) {
        downloadPdfBtn.addEventListener('click', downloadPdf);
    }

    // Valores iniciales
    updateDaysValue();
    updateBudgetValue();
});