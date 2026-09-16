const CACHE_NAME = 'vvault-v2';
const APP_SHELL = [
  './index.html',
  './manifest.json',
  './icon-192.png',
  './icon-512.png',
  './icon-180.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL))
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    )
  );
  self.clients.claim();
});

// NETWORK-FIRST: always try to fetch the latest version first. Only fall back
// to the cached copy if the network request fails (i.e. truly offline).
// This is what makes updates show up immediately instead of being stuck on
// whatever was cached the very first time the app was installed.
self.addEventListener('fetch', (event) => {
  const req = event.request;
  event.respondWith(
    fetch(req)
      .then((res) => {
        if (req.method === 'GET' && res && res.status === 200) {
          const resClone = res.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(req, resClone));
        }
        return res;
      })
      .catch(() => caches.match(req))
  );
});
