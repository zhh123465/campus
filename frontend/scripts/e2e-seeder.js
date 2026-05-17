import axios from 'axios';

const BASE_URL = 'http://localhost:8080/api/v1';

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // If the error is a 400, it might be expected for boundary tests.
    return Promise.reject(error);
  }
);

// Constants
const TENANT_ID = 1; // Default tenant
const NUM_USERS = 50;
const NUM_POSTS = 200;
const BOUNDARY_USERS = 5;

// Memory storage
const users = []; // [{ email, password, token, user_id }]
const spaces = [];
const posts = [];

// Helper to wait
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function main() {
  console.log('🚀 Starting E2E Seeder and Integration Test...');

  try {
    await testHealth();
    await createBoundaryUsers();
    await generateUsers();
    await createSpaces();
    await generatePosts();
    await simulateInteractions();
    
    console.log('✅ Seeding and Testing Completed Successfully!');
  } catch (error) {
    console.error('❌ E2E Seeder Failed:', error.message);
    if (error.response) {
      console.error('Response Data:', error.response.data);
    }
  }
}

async function testHealth() {
  console.log('=> Testing backend health...');
  // Just wait a bit and try reaching an open endpoint.
  // Assuming there's a ping or we just try a generic GET /posts
  try {
    await api.get('/posts', { params: { current: 1, pageSize: 1, tenantId: TENANT_ID } });
    console.log('   Backend is reachable.');
  } catch (error) {
    if (error.response) {
      console.log('   Backend is reachable (returned ' + error.response.status + ').');
    } else {
      console.error('   Backend unreachable. Please ensure it is running.');
      throw error;
    }
  }
}

async function createBoundaryUsers() {
  console.log('=> Testing boundary conditions for user creation...');
  const promises = [];
  
  for (let i = 0; i < BOUNDARY_USERS; i++) {
    const longName = 'A'.repeat(300); // Exceed typical nickname length
    const payload = {
      email: `boundary_user_${i}@example.com`,
      password: 'password123',
      nickname: longName,
      tenantId: TENANT_ID
    };
    
    promises.push(
      api.post('/auth/register', payload)
        .then(() => {
          console.warn(`   WARNING: Boundary user ${i} registered successfully (expected fail due to length).`);
        })
        .catch(err => {
          if (err.response && err.response.status === 400) {
            console.log(`   Boundary user ${i} successfully rejected with 400.`);
          } else {
            console.warn(`   WARNING: Boundary user ${i} failed with non-400 status:`, err.response?.status);
          }
        })
    );
  }
  await Promise.all(promises);
}

async function generateUsers() {
  console.log(`=> Generating ${NUM_USERS} normal users...`);
  const promises = [];
  const startIdx = Date.now() % 10000; // random offset

  for (let i = 0; i < NUM_USERS; i++) {
    const email = `testuser_${startIdx}_${i}@example.com`;
    const password = 'password123';
    const nickname = `Tester ${startIdx}-${i}`;
    
    promises.push(
      api.post('/auth/register', { email, password, nickname, tenantId: TENANT_ID })
        .then(async (res) => {
          // Attempt to login to get a token
          const loginRes = await api.post('/auth/login', { email, password, tenantId: TENANT_ID });
          const token = loginRes.data.data?.token || loginRes.data.token;
          const id = loginRes.data.data?.user?.id || loginRes.data.user?.id;
          users.push({ email, password, token, id });
        })
        .catch(err => {
          console.error(`   Failed to create user ${email}:`, err.response?.data?.message || err.message);
        })
    );
    
    // Batch to avoid overwhelming locally too fast
    if (promises.length >= 10) {
      await Promise.all(promises);
      promises.length = 0;
      process.stdout.write('.');
    }
  }
  
  if (promises.length > 0) {
    await Promise.all(promises);
  }
  console.log(`\n   Created and logged in ${users.length} users.`);
}

async function createSpaces() {
  console.log('=> Creating spaces...');
  // Use the first user as admin or just try to create spaces
  if (users.length === 0) return;
  const adminToken = users[0].token;
  const config = { headers: { Authorization: adminToken } };
  
  const spaceNames = ['Campus Life', 'Study & Resources', 'Marketplace', 'Tech Talk', 'Events'];
  
  for (const name of spaceNames) {
    try {
      const res = await api.post('/spaces', {
        name: name,
        description: `Discussion for ${name}`,
        tenantId: TENANT_ID
      }, config);
      const spaceId = res.data.data?.id || res.data?.id;
      if (spaceId) {
        spaces.push(spaceId);
      }
    } catch (err) {
      console.warn(`   Could not create space '${name}' (might already exist or need admin): ${err.response?.status}`);
    }
  }
  
  // Fetch existing spaces if any
  try {
    const res = await api.get('/spaces', { params: { tenantId: TENANT_ID } });
    const fetched = res.data.data || res.data;
    if (Array.isArray(fetched)) {
      fetched.forEach(s => {
        if (!spaces.includes(s.id)) spaces.push(s.id);
      });
    }
  } catch (err) {}
  
  console.log(`   Available spaces: ${spaces.length}`);
}

async function generatePosts() {
  console.log(`=> Generating ${NUM_POSTS} posts...`);
  if (users.length === 0) return;
  
  const promises = [];
  
  for (let i = 0; i < NUM_POSTS; i++) {
    // Pick a random user
    const user = users[Math.floor(Math.random() * users.length)];
    const spaceId = spaces.length > 0 ? spaces[Math.floor(Math.random() * spaces.length)] : null;
    
    let content = `This is randomly generated post #${i}. Hello world! `;
    if (i % 5 === 0) {
      content += `\n# Markdown Title\n- Bullet 1\n- Bullet 2\n**Bold Text**`;
    }
    if (i % 10 === 0) {
      // Boundary testing: very long content
      content += 'A'.repeat(5000);
    }
    
    const payload = {
      title: `Post Title ${i}`,
      content: content,
      spaceId: spaceId,
      tenantId: TENANT_ID
    };
    
    const config = { headers: { Authorization: user.token } };
    
    promises.push(
      api.post('/posts', payload, config)
        .then(res => {
          const postId = res.data.data?.id || res.data?.id;
          if (postId) posts.push(postId);
        })
        .catch(err => {
          if (i % 10 !== 0) {
            console.error(`   Failed to create post ${i}:`, err.response?.data?.message || err.message);
          }
        })
    );
    
    if (promises.length >= 20) {
      await Promise.all(promises);
      promises.length = 0;
      process.stdout.write('.');
    }
  }
  
  if (promises.length > 0) {
    await Promise.all(promises);
  }
  
  console.log(`\n   Created ${posts.length} posts.`);
}

async function simulateInteractions() {
  console.log('=> Simulating likes and comments...');
  if (users.length === 0 || posts.length === 0) return;
  
  const promises = [];
  // 300 random likes/comments
  for (let i = 0; i < 300; i++) {
    const user = users[Math.floor(Math.random() * users.length)];
    const postId = posts[Math.floor(Math.random() * posts.length)];
    const isLike = Math.random() > 0.5;
    
    const config = { headers: { Authorization: user.token } };
    
    if (isLike) {
      promises.push(
        api.post(`/posts/${postId}/reactions`, {
          targetType: 'POST',
          targetId: postId,
          type: 'LIKE'
        }, config).catch(() => {})
      );
    } else {
      promises.push(
        api.post('/comments', {
          targetType: 'POST',
          targetId: postId,
          content: `Random comment ${i} from user!`,
          tenantId: TENANT_ID
        }, config).catch(() => {})
      );
    }
    
    if (promises.length >= 30) {
      await Promise.all(promises);
      promises.length = 0;
      process.stdout.write('.');
    }
  }
  
  if (promises.length > 0) {
    await Promise.all(promises);
  }
  
  console.log('\n   Finished simulating interactions.');
}

main();
