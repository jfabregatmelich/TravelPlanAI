// URL de la API backend (Spring Boot en puerto 8080)
const API_BASE_URL = 'http://localhost:8080/api';

// Mapeo de paises a sus ciudades principales
const countryCitiesMap = {
    "Alemania": ["Berlín", "Múnich", "Hamburgo", "Colonia", "Fráncfort", "Stuttgart"],
    "Austria": ["Viena", "Salzburgo", "Innsbruck", "Graz"],
    "Bélgica": ["Bruselas", "Amberes", "Gante", "Brujas"],
    "Bulgaria": ["Sofía", "Plovdiv", "Varna"],
    "Croacia": ["Zagreb", "Split", "Dubrovnik", "Rijeka"],
    "Dinamarca": ["Copenhague", "Aarhus", "Odense"],
    "Eslovaquia": ["Bratislava", "Košice"],
    "Eslovenia": ["Liubliana", "Maribor"],
    "España": ["Madrid", "Barcelona", "Sevilla", "Valencia", "Bilbao", "Granada"],
    "Estonia": ["Tallin", "Tartu"],
    "Finlandia": ["Helsinki", "Tampere", "Turku"],
    "Francia": ["París", "Lyon", "Marsella", "Burdeos", "Niza", "Estrasburgo"],
    "Grecia": ["Atenas", "Tesalónica", "Heraclión", "Patras"],
    "Hungría": ["Budapest", "Debrecen", "Szeged"],
    "Irlanda": ["Dublín", "Cork", "Galway"],
    "Italia": ["Roma", "Florencia", "Venecia", "Milán", "Nápoles", "Turín"],
    "Letonia": ["Riga", "Daugavpils"],
    "Lituania": ["Vilna", "Kaunas"],
    "Luxemburgo": ["Luxemburgo"],
    "Malta": ["La Valeta", "Sliema"],
    "Países Bajos": ["Ámsterdam", "Róterdam", "La Haya", "Utrecht"],
    "Polonia": ["Varsovia", "Cracovia", "Gdańsk", "Breslavia"],
    "Portugal": ["Lisboa", "Oporto", "Faro", "Coímbra", "Braga"],
    "Reino Unido": ["Londres", "Edimburgo", "Mánchester", "Liverpool", "Glasgow"],
    "República Checa": ["Praga", "Brno", "Ostrava"],
    "Rumanía": ["Bucarest", "Cluj-Napoca", "Timișoara"],
    "Suecia": ["Estocolmo", "Gotemburgo", "Malmö"],
    "Suiza": ["Zúrich", "Ginebra", "Berna", "Lucerna", "Basilea"]
};

// Espera a que el DOM esté completamente cargado antes de ejecutar el codigo
document.addEventListener('DOMContentLoaded', function() {
    
    // Elementos del DOM
    const travelForm = document.getElementById('travelForm');
    const countrySelect = document.getElementById('country');
    const citySelect = document.getElementById('city');
    const daysSlider = document.getElementById('days');
    const daysValue = document.getElementById('daysValue');
    const budgetPerDaySlider = document.getElementById('budgetPerDay');
    const budgetPerDayValue = document.getElementById('budgetPerDayValue');
    const generateBtn = document.getElementById('generateBtn');
    const loadingDiv = document.getElementById('loading');
    const errorDiv = document.getElementById('error');
    const resultsDiv = document.getElementById('results');
    const planSummaryDiv = document.getElementById('planSummary');
    const downloadPdfBtn = document.getElementById('downloadPdfBtn');
    const randomToggle = document.getElementById('randomToggle');

    let currentTravelPlan = null;
    let isRandomMode = false;
    let currentRandomPreview = null;

    // Configurar sliders con labels posicionadas exactamente
    function setupRangeLabels() {
        setupSingleRangeLabels('days', 1, 10, [1, 3, 5, 7, 10], 'días');
        setupSingleRangeLabels('budgetPerDay', 20, 200, [20, 50, 80, 120, 160, 200], '€/día');
    }

    function setupSingleRangeLabels(sliderId, min, max, labelValues, unit) {
        const container = document.getElementById(sliderId).closest('.form-group');
        const labelsContainer = container.querySelector('.range-labels');
        if (!labelsContainer) return;
        
        labelsContainer.innerHTML = '';
        const range = max - min;
        
        labelValues.forEach(value => {
            const percent = ((value - min) / range) * 100;
            const label = document.createElement('span');
            label.className = 'range-label';
            label.textContent = value + (unit === 'días' ? 'd' : '€');
            label.style.left = `${percent}%`;
            labelsContainer.appendChild(label);
        });
    }

    // Actualiza el valor mostrado del slider de dias
    function updateDaysValue() {
        daysValue.textContent = daysSlider.value;
    }

    // Actualiza el valor mostrado del slider de presupuesto por dia
    function updateBudgetPerDayValue() {
        budgetPerDayValue.textContent = budgetPerDaySlider.value;
    }

    // Actualiza las ciudades disponibles basado en el pais seleccionado
    function updateCityOptions() {
        if (isRandomMode) {
            citySelect.innerHTML = '<option value="">-- Modo sorpresa activado --</option>';
            citySelect.disabled = true;
            citySelect.required = false;
            return;
        }

        const selectedCountry = countrySelect.value;
        
        if (!selectedCountry) {
            citySelect.innerHTML = '<option value="">-- Primero selecciona un país --</option>';
            citySelect.disabled = true;
            citySelect.required = false;
            return;
        }
        
        let cities = countryCitiesMap[selectedCountry];
        if (!cities || cities.length === 0) {
            cities = [selectedCountry + " capital"];
        }
        
        citySelect.innerHTML = '<option value="">Selecciona una ciudad</option>';
        cities.forEach(city => {
            const option = document.createElement('option');
            option.value = city;
            option.textContent = city;
            citySelect.appendChild(option);
        });
        
        citySelect.disabled = false;
        citySelect.required = true;
        
        if (cities.length > 0) {
            citySelect.value = cities[0];
        }
    }

    // Genera un destino aleatorio (pais aleatorio + ciudad aleatoria)
    function getRandomDestination() {
        const countryList = Object.keys(countryCitiesMap).sort();
        if (countryList.length === 0) return { country: "Francia", city: "París" };
        
        const randomCountry = countryList[Math.floor(Math.random() * countryList.length)];
        const citiesForCountry = countryCitiesMap[randomCountry];
        const randomCity = citiesForCountry[Math.floor(Math.random() * citiesForCountry.length)];
        
        return { country: randomCountry, city: randomCity };
    }

    // Mostrar preview del destino aleatorio
    function showRandomPreview() {
        if (currentRandomPreview) {
            currentRandomPreview.remove();
        }
        
        const randomDest = getRandomDestination();
        const randomizerDiv = document.querySelector('.randomizer-header');
        
        const preview = document.createElement('div');
        preview.className = 'random-preview';
        preview.innerHTML = `<i class="fas fa-map-marked-alt"></i> Destino sorpresa: <strong>${randomDest.city}, ${randomDest.country}</strong> <small>(al generar)</small>`;
        
        randomizerDiv.insertAdjacentElement('afterend', preview);
        currentRandomPreview = preview;
        
        return randomDest;
    }

    // Actualizar preview cada vez que cambia el modo o periódicamente
    function refreshRandomPreview() {
        if (isRandomMode) {
            showRandomPreview();
        }
    }

    // Maneja cambio de modo aleatorio - SOLO deshabilita pais y ciudad, NO los sliders ni el boton
    function toggleRandomMode() {
        isRandomMode = randomToggle.checked;
        
        if (isRandomMode) {
            // Deshabilitar SOLO pais y ciudad
            countrySelect.disabled = true;
            citySelect.disabled = true;
            citySelect.required = false;
            citySelect.innerHTML = '<option value="">-- Modo sorpresa: destino aleatorio --</option>';
            countrySelect.value = "";
            
            // Los sliders y el boton permanecen HABILITADOS
            daysSlider.disabled = false;
            budgetPerDaySlider.disabled = false;
            generateBtn.disabled = false;
            
            // Restaurar opacidad de sliders por si acaso
            daysSlider.style.opacity = '1';
            budgetPerDaySlider.style.opacity = '1';
            
            // Mostrar preview del destino aleatorio
            showRandomPreview();
        } else {
            // Re-habilitar pais y ciudad
            countrySelect.disabled = false;
            
            if (currentRandomPreview) {
                currentRandomPreview.remove();
                currentRandomPreview = null;
            }
            
            if (countrySelect.value) {
                updateCityOptions();
            } else {
                citySelect.innerHTML = '<option value="">-- Primero selecciona un país --</option>';
                citySelect.disabled = true;
                citySelect.required = false;
            }
        }
    }

    // Muestra un mensaje de error en el interfaz
    function showError(message) {
        errorDiv.innerHTML = `<i class="fas fa-exclamation-triangle"></i> ${message}`;
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
        let html = `<div class="destination-header"><i class="fas fa-map-pin"></i> <strong>Destino:</strong> ${plan.destination || 'Personalizado'}</div>`;
        html += `<p><i class="far fa-clock"></i> <strong>Días:</strong> ${plan.totalDays}</p>`;
        html += `<p><i class="fas fa-euro-sign"></i> <strong>Presupuesto estimado por día:</strong> ${plan.budgetPerDay || Math.round(plan.totalBudget / plan.totalDays)} €/día</p>`;
        html += `<p><i class="fas fa-coins"></i> <strong>Presupuesto total estimado:</strong> ${plan.totalBudget} €</p>`;
        
        if (plan.itinerary && plan.itinerary.length > 0) {
            html += `<h3><i class="fas fa-sun"></i> Itinerario diario</h3>`;
            plan.itinerary.forEach(day => {
                html += `<div class="day-card">`;
                html += `<h4><i class="far fa-calendar-check"></i> Día ${day.day}</h4>`;
                if (day.morning) {
                    html += `<p><strong><i class="fas fa-cloud-sun"></i> Mañana:</strong> ${day.morning}</p>`;
                }
                if (day.lunch) {
                    html += `<p><strong><i class="fas fa-utensils"></i> Comida:</strong> ${day.lunch}</p>`;
                }
                if (day.afternoon) {
                    html += `<p><strong><i class="fas fa-tree"></i> Tarde:</strong> ${day.afternoon}</p>`;
                }
                html += `</div>`;
            });
        } else if (plan.description) {
            html += `<div class="day-card"><p>${plan.description}</p></div>`;
        }
        
        planSummaryDiv.innerHTML = html;
        resultsDiv.classList.remove('hidden');
        resultsDiv.scrollIntoView({ behavior: 'smooth', block: 'start' });
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
            showError('No se pudo generar el plan de viaje. Asegurate de que el backend esta ejecutandose en el puerto 8080.');
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

    // Añadir atributos ARIA para accesibilidad
    function setupAccessibility() {
        daysSlider.setAttribute('aria-label', 'Selector de duración del viaje en días');
        daysSlider.setAttribute('aria-valuemin', daysSlider.min);
        daysSlider.setAttribute('aria-valuemax', daysSlider.max);
        daysSlider.setAttribute('aria-valuenow', daysSlider.value);
        
        budgetPerDaySlider.setAttribute('aria-label', 'Selector de presupuesto por día en euros');
        budgetPerDaySlider.setAttribute('aria-valuemin', budgetPerDaySlider.min);
        budgetPerDaySlider.setAttribute('aria-valuemax', budgetPerDaySlider.max);
        budgetPerDaySlider.setAttribute('aria-valuenow', budgetPerDaySlider.value);
        
        daysSlider.addEventListener('input', () => {
            daysSlider.setAttribute('aria-valuenow', daysSlider.value);
        });
        
        budgetPerDaySlider.addEventListener('input', () => {
            budgetPerDaySlider.setAttribute('aria-valuenow', budgetPerDaySlider.value);
        });
    }

    // Eventos de los sliders
    daysSlider.addEventListener('input', updateDaysValue);
    budgetPerDaySlider.addEventListener('input', updateBudgetPerDayValue);

    // Evento cambio de pais
    countrySelect.addEventListener('change', function() {
        if (!isRandomMode) {
            updateCityOptions();
        }
    });

    // Evento del toggle aleatorio
    randomToggle.addEventListener('change', toggleRandomMode);

    // Evento de envio del formulario
    travelForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        let requestData;
        
        if (isRandomMode) {
            // Modo sorpresa: obtener destino aleatorio
            const randomDest = getRandomDestination();
            const days = parseInt(daysSlider.value);
            const budgetPerDay = parseInt(budgetPerDaySlider.value);
            const totalBudget = days * budgetPerDay;
            
            requestData = {
                countries: [randomDest.country],
                city: randomDest.city,
                days: days,
                budget: totalBudget,
                budgetPerDay: budgetPerDay
            };
        } else {
            // Modo normal: validar campos
            let selectedCountry = countrySelect.value;
            let city = citySelect.value;
            
            if (!selectedCountry) {
                showError('Por favor selecciona un país');
                return;
            }
            
            if (!city) {
                showError('Por favor selecciona una ciudad');
                return;
            }
            
            const days = parseInt(daysSlider.value);
            const budgetPerDay = parseInt(budgetPerDaySlider.value);
            const totalBudget = days * budgetPerDay;
            
            requestData = {
                countries: [selectedCountry],
                city: city,
                days: days,
                budget: totalBudget,
                budgetPerDay: budgetPerDay
            };
        }
        
        showLoading();
        await generateTravelPlan(requestData);
        hideLoading();
    });

    // Evento del boton de descarga de PDF
    if (downloadPdfBtn) {
        downloadPdfBtn.addEventListener('click', downloadPdf);
    }
    
    // Inicializacion
    setupRangeLabels();
    setupAccessibility();
    
    updateDaysValue();
    updateBudgetPerDayValue();
    
    // Inicializar estado
    citySelect.disabled = true;
    citySelect.required = false;
    randomToggle.checked = false;
    isRandomMode = false;
    countrySelect.disabled = false;
    daysSlider.disabled = false;
    budgetPerDaySlider.disabled = false;
    generateBtn.disabled = false;
});