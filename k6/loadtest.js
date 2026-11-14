import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

http.setResponseCallback(http.expectedStatuses(200, 429));

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const allowed = new Counter('allowed_200');
const blocked = new Counter('blocked_429');

export const options = {
  scenarios: {
    burst: {
      executor: 'constant-arrival-rate',
      rate: 200,
      timeUnit: '1s',
      duration: '10s',
      preAllocatedVUs: 50,
      maxVUs: 100,
    },
  },
  thresholds: {
    allowed_200: ['count<=25'],
  },
};

export default function () {
  const res = http.get(`${BASE}/api/hello`, {
    headers: { 'X-API-Key': 'demo-key' },
  });
  if (res.status === 200) allowed.add(1);
  if (res.status === 429) blocked.add(1);
  check(res, { 'status is 200 or 429': (r) => r.status === 200 || r.status === 429 });
}