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

let lastData = null;
let unitMode = localStorage.getItem('unitMode') || 'metric'; // metric: °C+km/h，imperial: °F+mph

function describeWeather(code) {
  return WEATHER_CODES[code] || ['❓', '未知'];
}

function formatTemp(celsius) {
  return unitMode === 'imperial'
    ? (celsius * 9 / 5 + 32).toFixed(1) + '°F'
    : celsius.toFixed(1) + '°C';
}

function formatTempShort(celsius) {
  return unitMode === 'imperial'
    ? Math.round(celsius * 9 / 5 + 32) + '°'
    : Math.round(celsius) + '°';
}

function formatWind(kmh) {
  return unitMode === 'imperial'
    ? (kmh * 0.621371).toFixed(1) + ' mph'
    : kmh.toFixed(1) + ' km/h';
}

// ---------- 单位切换 ----------

function toggleUnit() {
  unitMode = unitMode === 'metric' ? 'imperial' : 'metric';
  localStorage.setItem('unitMode', unitMode);
  updateUnitToggleLabel();
  if (lastData) renderWeather(lastData);
}

function updateUnitToggleLabel() {
  document.getElementById('unitToggle').textContent = unitMode === 'imperial' ? '°F · mph' : '°C · km/h';
}

// ---------- 收藏（localStorage） ----------

function loadFavorites() {
  try {
    return JSON.parse(localStorage.getItem('favoriteCities')) || [];
  } catch (e) {
    return [];
  }
}

function saveFavorites(list) {
  localStorage.setItem('favoriteCities', JSON.stringify(list));
}

function toggleFavorite(city) {
  let list = loadFavorites();
  list = list.includes(city) ? list.filter(c => c !== city) : [...list, city];
  saveFavorites(list);
  renderFavorites();
  if (lastData) renderWeather(lastData); // 刷新卡片上的 ☆/★
}

function toggleFavoriteLastCity() {
  if (lastData && lastData.city && lastData.city !== '当前位置') {
    toggleFavorite(lastData.city);
  }
}

function renderFavorites() {
  const list = loadFavorites();
  const box = document.getElementById('favorites');
  if (list.length === 0) {
    box.innerHTML = '';
    box.classList.add('hidden');
    return;
  }
  box.innerHTML = list.map(c =>
    `<span class="fav-chip" data-city="${c}">${c}<span class="fav-remove" data-remove="${c}">×</span></span>`
  ).join('');
  box.classList.remove('hidden');
}

// ---------- 查询 ----------

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

function queryByLocation() {
  if (!navigator.geolocation) {
    showError('当前浏览器不支持定位');
    return;
  }
  const btn = document.getElementById('locateBtn');
  btn.disabled = true;
  navigator.geolocation.getCurrentPosition(async pos => {
    try {
      const lat = pos.coords.latitude.toFixed(4);
      const lon = pos.coords.longitude.toFixed(4);
      const res = await fetch(`/api/weather/location?lat=${lat}&lon=${lon}`);
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
    }
  }, () => {
    showError('定位失败：请在浏览器中允许获取位置');
    btn.disabled = false;
  });
}

// ---------- 渲染 ----------

function renderWeather(data) {
  lastData = data;
  hideError();
  const [icon, desc] = describeWeather(data.current.weatherCode);
  const isFav = loadFavorites().includes(data.city);
  const favBtn = data.city && data.city !== '当前位置'
    ? `<button class="fav-star" onclick="toggleFavoriteLastCity()" title="收藏/取消收藏">${isFav ? '★' : '☆'}</button>`
    : '';
  const currentCard = document.getElementById('currentCard');
  currentCard.innerHTML = `
    <h2>${data.city}${data.country ? ' · ' + data.country : ''}${favBtn}${data.cached ? ' <span class="cached-tag">缓存</span>' : ''}</h2>
    <div class="current-main">
      <span class="current-icon">${icon}</span>
      <span class="current-temp">${formatTemp(data.current.temperature)}</span>
      <span class="current-desc">${desc}</span>
    </div>
    <div class="current-details">
      <span>体感 ${formatTemp(data.current.feelsLike)}</span>
      <span>湿度 ${data.current.humidity}%</span>
      <span>风速 ${formatWind(data.current.windSpeed)}</span>
    </div>`;
  currentCard.classList.remove('hidden');

  const dailyList = document.getElementById('dailyList');
  dailyList.innerHTML = data.daily.map(d => {
    const [dIcon, dDesc] = describeWeather(d.weatherCode);
    return `<div class="daily-item">
      <span class="daily-date">${d.date}</span>
      <span class="daily-icon">${dIcon}</span>
      <span class="daily-desc">${dDesc}</span>
      <span class="daily-temp">${formatTempShort(d.tempMin)} / ${formatTempShort(d.tempMax)}</span>
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

// ---------- 初始化 ----------

document.getElementById('queryBtn').addEventListener('click', queryWeather);
document.getElementById('locateBtn').addEventListener('click', queryByLocation);
document.getElementById('unitToggle').addEventListener('click', toggleUnit);
document.getElementById('cityInput').addEventListener('keydown', e => {
  if (e.key === 'Enter') queryWeather();
});
document.getElementById('favorites').addEventListener('click', e => {
  const remove = e.target.closest('[data-remove]');
  if (remove) {
    toggleFavorite(remove.dataset.remove);
    return;
  }
  const chip = e.target.closest('[data-city]');
  if (chip) {
    document.getElementById('cityInput').value = chip.dataset.city;
    queryWeather();
  }
});

updateUnitToggleLabel();
renderFavorites();
