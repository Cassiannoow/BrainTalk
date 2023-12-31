import express from 'express';
import { initializeApp } from "firebase/app";
import admin from 'firebase-admin';
import bodyParser from 'body-parser';
import { collection, doc, getDoc, getDocs, getFirestore, setDoc, deleteDoc, query, where } from 'firebase/firestore/lite';

const firebaseConfig = {
  apiKey: "AIzaSyBGO7desXzBhF0sGQd9WuVh_nzhQwW48gc",
  authDomain: "braintalk-d6eb3.firebaseapp.com",
  projectId: "braintalk-d6eb3",
  storageBucket: "braintalk-d6eb3.appspot.com",
  messagingSenderId: "357909634933",
  appId: "1:357909634933:web:669382ecfac3420ec67d3c",
  measurementId: "G-BWW2ZFLBHG"
};

const firebase = initializeApp(firebaseConfig);
const db = getFirestore(firebase);

const usersCol = collection(db, 'Users');
const friends = collection(db, 'Friends');
const posts = collection(db, 'Posts');
const likes = collection(db, 'Likes');

const validateUser = (user) => {
    try {
        if (!user.id || !user.email || !user.name || !user.password) {
            return false;
        }
        return user.id.length >= 4 && user.id.length <= 15 &&
            user.email.length >= 5 && user.email.length <= 30 && user.email.includes('@') &&
            user.name.length >= 3 && user.name.length <= 30 &&
            user.password.length >= 8 && user.password.length <= 30 &&
            user.username.lenght >= 1
    } catch (error) {
        console.error('Error validating user: ', error);
        return false;
    }
};


const existLikePost = async (postId, username) => {
    try {
        const data = await getDocs(likes);
        return data.docs.some((doc) =>{
            if(doc.data().postId === postId )
            {
                if(doc.data().username === username)
                {
                    return true
                }
            }
            return false
        })
    } catch (e) {
        console.error('Error checking if user exists: ', error);
        return false;
    }
}

const existsUser = async (id) => {
    try {
        const data = await getDocs(usersCol);
        return data.docs.some((doc) => doc.data().id === id);
    } catch (error) {
        console.error('Error checking if user exists: ', error);
        return false;
    }
};
const existsUserEmail = async (email) => {
    try {
        const data = await getDocs(usersCol);
        return data.docs.some((doc) => doc.data().email === email);
    } catch (error) {
        console.error('Error checking if user email exists: ', error);
        return false;
    }
};

const handleResponse = (res, status, message) => {
    res.status(status).send(message);
};

const app = express();
const port = 3000;

app.use(bodyParser.json({ limit: '10mb' }));
app.use(bodyParser.urlencoded({ limit: '10mb', extended: true }));
app.use(express.json());

/*app.get('/users', async (req, res) => {
    const users = await getDocs(usersCol);
    res.send(users.docs.map((doc) => doc.data()));
});

app.get('/user/:id', async (req, res) => {
    const id = req.params.id;
    const users = await getDocs(usersCol);
    const user = users.docs.find((doc) => doc.data().id === id);
    if (user) {
        res.send(user.data());
    } else {
        handleResponse(res, 404, 'User does not exist');
    }
});*/

app.get('/posts/', async (req, res) => {
    try {
        const playersSnapshot = await getDocs(posts);
        const postsData = [];

        playersSnapshot.forEach((postDoc) => {
            const postId = postDoc.id;

            // Obtém os dados do documento
            const postData = postDoc.data();

            // Adiciona o ID ao objeto de dados
            postsData.push({ id: postId, ...postData });
        });

        res.status(200).send(postsData);
    } catch (error) {
        console.error('Error retrieving posts: ', error);
        handleResponse(res, 500, 'Internal Server Error');
    }
});

// ...

app.get('/postssortbylike', async (req, res) => {
    try {
        const postsSnapshot = await getDocs(posts);
        const postsData = [];

        // Itera sobre os documentos da coleção Posts   
        for (const postDoc of postsSnapshot.docs) {
            const postId = postDoc.id;

            // Obtém os dados do documento
            const postData = postDoc.data();

            // Obtém as curtidas associadas ao post
            const likesQuery = query(likes, where('postId', '==', postId));
            const likesSnapshot = await getDocs(likesQuery);
            const likesCount = likesSnapshot.size;

            // Adiciona o ID e o número de curtidas ao objeto de dados
            postsData.push({ id: postId, likes: likesCount, ...postData });
        }

        // Ordena os posts por número de curtidas (do maior para o menor)
        postsData.sort((a, b) => b.likes - a.likes);

        res.status(200).send(postsData);
    } catch (error) {
        console.error('Error retrieving posts: ', error);
        handleResponse(res, 500, 'Internal Server Error');
    }
});

// ...


admin.initializeApp();

const a = admin.firestore();

app.post('/post', async (req, res) => {
    try {
        const newUser = req.body;

        // Converte o valor Long de dataPost para um objeto Timestamp do Firestore
        const dataPostTimestamp = admin.firestore.Timestamp.fromMillis(newUser.dataPost);

        // Atualiza o valor de dataPost no objeto newUser
        newUser.dataPost = dataPostTimestamp.toDate();
        await setDoc(doc(db, 'Posts', newUser.id), newUser);
        handleResponse(res, 200, {msg: 'Post added'})
    } catch (error) {
        console.error('Error adding user:', error);
        handleResponse(res, 500, { msg: 'Internal Server Error' });
    }
});
app.get('/likes/', async (req, res) => {
    try {
        const playersSnapshot = await getDocs(likes);
        const friendsLists = [];

        playersSnapshot.forEach((playerDoc) => {
            const likeData = playerDoc.data();
            const likeId = playerDoc.id;
            friendsLists.push({ id: likeId, ...likeData});
        });

        res.status(200).send(friendsLists);
    } catch (error) {
        console.error('Error retrieving likes: ', error);
        handleResponse(res, 500, 'Internal Server Error');
    }
});

app.post('/like', async (req, res) => {
    try {
        const newLike = req.body;

        // Verificar se o post (dataPost) já tem um like deste usuário
        const postAlreadyLiked = await existLikePost(newLike.postId, newLike.username);
        if (postAlreadyLiked) {
            console.log("O post já foi curtido por este usuário");
            return res.status(400).send({ msg: 'Post already liked by this user' });
        }

        // Adicionar o like
        await setDoc(doc(db, 'Likes', newLike.id), newLike);
        handleResponse(res, 200, { msg: 'Like added' });
    } catch (error) {
        console.error('Error adding like:', error);
        handleResponse(res, 500, { msg: 'Internal Server Error' });
    }
});



app.delete('/like/:id', async (req, res) => {
    const id = req.params.id;
    await deleteDoc(doc(db, 'Likes', id), id)
    res.status(200).send("Excluido");
})

app.get('/users/', async (req, res) => {
    try {
        const playersSnapshot = await getDocs(usersCol);
        const usersList = [];

        playersSnapshot.forEach((playerDoc) => {
            usersList.push(playerDoc.data(), playerDoc.id);
        });

        res.status(200).send(usersList);
    } catch (error) {
        console.error('Error retrieving players: ', error);
        handleResponse(res, 500, 'Internal Server Error');
    }
});

app.get('/users/:id', async (req, res) => {
    const id = req.params.id;
    try {
        const playerRef = doc(usersCol, id); // Get the specific player document by ID
        const playerDoc = await getDoc(playerRef);

        if (playerDoc.exists()) {
            res.status(200).send(playerDoc.data());
        } else {
            handleResponse(res, 404, 'Player does not exist');
        }
    } catch (error) {
        console.error('Error retrieving player: ', error);
        handleResponse(res, 500, 'Internal Server Error');
    }
});

app.get('/friends/', async (req, res) => {
    try {
        const playersSnapshot = await getDocs(friends);
        const friendsLists = [];

        playersSnapshot.forEach((playerDoc) => {
            friendsLists.push(playerDoc.data());
        });

        res.status(200).send(friendsLists);
    } catch (error) {
        console.error('Error retrieving players: ', error);
        handleResponse(res, 500, 'Internal Server Error');
    }
});

app.post('/user', async (req, res) => {
    const newUser = req.body;
    await setDoc(doc(db, 'Users', newUser.username), newUser);
    handleResponse(res, 200, {msg: 'User added'})
    }
);

/*app.put('/user/:id', async (req, res) => {
    const id = req.params.id;
    const updatedUser = req.body;
    if (!validateUser(updatedUser)) {
        handleResponse(res, 400, 'Invalid input data');
    } else {
        const exists = await existsUser(id);
        if (exists) {
            await setDoc(doc(db, 'user', id), updatedUser);
            handleResponse(res, 200, 'User updated');
        } else {
            handleResponse(res, 404, 'User does not exist');
        }
    }
});*/

/*app.delete('/user/:id', async (req, res) => {
    const id = req.params.id;
    const exists = await existsUser(id);
    if (exists) {
        await deleteDoc(doc(db, 'user', id));
        const trainings = await getDocs(trainingsCol);
        trainings.forEach(async (doc) => {
            if (doc.data().usuario === id) {
                await deleteDoc(doc.ref);
                const exercises = await getDocs(exercisesCol);
                exercises.forEach(async (exercise) => {
                    if (exercise.data().treino === doc.data().id) {
                        await deleteDoc(exercise.ref);
                    }
                });
            }
        });
        handleResponse(res, 200, 'User deleted');
    } else {
        handleResponse(res, 404, 'User does not exist');
    }
});*/

app.use((req, res) => {
    handleResponse(res, 404, '404 Not Found');
});

app.listen(port, () => {
    console.log(`Example app listening at http://localhost:${port}`);
});
