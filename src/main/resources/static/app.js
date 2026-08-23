// WMO weatherCode → [表情, 中文描述]，完整表见 https://open-meteo.com/en/docs
const WEATHER_CODES = {
  0: ['☀️', '晴'], 1: ['🌤️', '大部晴朗'], 2: ['⛅', '多云'], 3: ['☁️', '阴'],
  45: ['🌫️', '雾'], 48: ['🌫️', '冻雾'],
  51: ['🌦️', '小毛毛雨'], 53: ['🌦️', '毛毛雨'], 55: ['🌧️', '大毛毛雨'],
  56: ['🌧️', '冻毛毛雨'], 57: ['🌧️', '强冻毛毛雨'],
  61: ['🌧️', '小雨'], 63: ['🌧️', '中雨'], 65: ['🌧️', '大雨'],
  66: ['🌧️', '冻雨'], 67: ['🌧️', '强冻雨'],
  71: ['🌨️', '小雪'], 73: ['🌨️', '中雪'], 75: ['❄️', '大雪'], 77: ['❄️', '雪粒'],
  80: ['🌦️', '小阵雨'], 81: ['🌧️', '阵雨'], 82: ['⛈️', '强阵雨'],
  85: ['🌨️', '小阵雪'], 86: ['❄️', '大阵雪'],
  95: ['⛈️', '雷暴'], 96: ['⛈️', '雷暴伴小冰雹'], 99: ['⛈️', '雷暴伴大冰雹']
};

function describeWeather(code) {
  return WEATHER_CODES[code] || ['❓', '未知'];
}

async function queryWeather() {
  const city = document.getElementById('cityInput').value.trim();
  if (!city) {
    showError('请输入城市名称');
    return;
  }
  const btn = document.getElementById('queryBtn');
  btn.disabled = true;
  btn.textContent = '查询中…';
  try {
    const res = await fetch(`/api/weather?city=${encodeURIComponent(city)}`);
    const data = await res.json();
    if (res.ok) {
      renderWeather(data);
    } else {
      showError(data.error);
    }
  } catch (e) {
    showError('无法连接服务器，请确认服务已启动');
  } finally {
    btn.disabled = false;
    btn.textContent = '查询';
  }
}

function renderWeather(data) {
  hideError();
  const [icon, desc] = describeWeather(data.current.weatherCode);
  const currentCard = document.getElementById('currentCard');
  currentCard.innerHTML = `
    <h2>${data.city} · ${data.country}${data.cached ? ' <span class="cached-tag">缓存</span>' : ''}</h2>
    <div class="current-main">
      <span class="current-icon">${icon}</span>
      <span class="current-temp">${data.current.temperature.toFixed(1)}°C</span>
      <span class="current-desc">${desc}</span>
    </div>
    <div class="current-details">
      <span>体感 ${data.current.feelsLike.toFixed(1)}°C</span>
      <span>湿度 ${data.current.humidity}%</span>
      <span>风速 ${data.current.windSpeed.toFixed(1)} km/h</span>
    </div>`;
  currentCard.classList.remove('hidden');

  const dailyList = document.getElementById('dailyList');
  dailyList.innerHTML = data.daily.map(d => {
    const [dIcon, dDesc] = describeWeather(d.weatherCode);
    return `<div class="daily-item">
      <span class="daily-date">${d.date}</span>
      <span class="daily-icon">${dIcon}</span>
      <span class="daily-desc">${dDesc}</span>
      <span class="daily-temp">${d.tempMin.toFixed(0)}° / ${d.tempMax.toFixed(0)}°</span>
      <span class="daily-precip">💧${d.precipProb}%</span>
    </div>`;
  }).join('');
  dailyList.classList.remove('hidden');
}

function showError(message) {
  const errorBox = document.getElementById('errorBox');
  errorBox.textContent = message;
  errorBox.classList.remove('hidden');
  document.getElementById('currentCard').classList.add('hidden');
  document.getElementById('dailyList').classList.add('hidden');
}

function hideError() {
  document.getElementById('errorBox').classList.add('hidden');
}

document.getElementById('queryBtn').addEventListener('click', queryWeather);
document.getElementById('cityInput').addEventListener('keydown', e => {
  if (e.key === 'Enter') queryWeather();
});
